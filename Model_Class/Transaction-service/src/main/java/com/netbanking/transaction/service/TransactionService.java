package com.netbanking.transaction.service;

import com.netbanking.transaction.client.AccountServiceClient;
import com.netbanking.transaction.client.AuthServiceClient;
import com.netbanking.transaction.client.UserServiceClient;
import com.netbanking.transaction.dto.request.TransferRequest;
import com.netbanking.transaction.dto.response.TransactionResponse;
import com.netbanking.transaction.entity.IdempotencyRecord;
import com.netbanking.transaction.entity.Transaction;
import com.netbanking.transaction.entity.TransactionStatusHistory;
import com.netbanking.transaction.entity.TransferDetails;
import com.netbanking.transaction.exception.DuplicateRequestException;
import com.netbanking.transaction.exception.ResourceNotFoundException;
import com.netbanking.transaction.kafka.producer.TransactionEventProducer;
import com.netbanking.transaction.repository.TransactionRepository;
import com.netbanking.transaction.repository.TransactionStatusHistoryRepository;
import com.netbanking.transaction.repository.TransferDetailsRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private static final BigDecimal MAX_TRANSFER_LIMIT = new BigDecimal("10000000.00");

    private final TransactionRepository transactionRepository;
    private final TransferDetailsRepository transferDetailsRepository;
    private final TransactionStatusHistoryRepository historyRepository;
    private final IdempotencyService idempotencyService;
    private final AccountServiceClient accountServiceClient;
    private final UserServiceClient userServiceClient;
    private final AuthServiceClient authServiceClient;
    private final TransactionEventProducer eventProducer;

    public TransactionResponse createTransfer(TransferRequest request, String customerId, String initiatedBy, String idempotencyKey) {
        return createTransfer(request, customerId, initiatedBy, null, idempotencyKey);
    }

    public TransactionResponse createTransfer(TransferRequest request, String customerId, String initiatedBy, String userEmail, String idempotencyKey) {
        // 1. Basic validation
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID header (X-Customer-Id) is required");
        }
        if (initiatedBy == null || initiatedBy.isBlank()) {
            initiatedBy = "CUSTOMER";
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero");
        }
        if (request.amount().compareTo(MAX_TRANSFER_LIMIT) > 0) {
            throw new IllegalArgumentException("Transfer amount exceeds maximum allowed limit of ₹1,00,00,000 (1 Crore INR)");
        }

        // 2. Enforce Account Number transfers for customers - reject Database ID transfers
        String destAccountNumber = request.destinationAccountNumber();
        if ("CUSTOMER".equalsIgnoreCase(initiatedBy)) {
            if (request.destinationAccountId() != null) {
                throw new IllegalArgumentException("Customer transfers using internal Database Account IDs are not permitted. Please use the beneficiary's Account Number starting with 'NB'.");
            }
            if (destAccountNumber == null || destAccountNumber.isBlank() || !destAccountNumber.trim().toUpperCase().startsWith("NB")) {
                throw new IllegalArgumentException("Customer fund transfers must be made using a valid NetBanking Account Number starting with 'NB' (e.g. NB221992311862871354). Transfers with Database IDs are not permitted.");
            }
        }

        // 3. Resolve destination account
        AccountServiceClient.AccountResponse destAcc;
        if (destAccountNumber != null && !destAccountNumber.isBlank()) {
            try {
                destAcc = accountServiceClient.getByAccountNumber(destAccountNumber.trim().toUpperCase());
            } catch (Exception ex) {
                throw new ResourceNotFoundException("Beneficiary account number not found: " + destAccountNumber);
            }
            if (destAcc == null || destAcc.accountId() == null) {
                throw new ResourceNotFoundException("Beneficiary account number not found: " + destAccountNumber);
            }
            if (destAcc.accountStatus() != null && !"ACTIVE".equalsIgnoreCase(destAcc.accountStatus())) {
                throw new IllegalStateException("Destination account is not ACTIVE (Status: " + destAcc.accountStatus() + ")");
            }
            if (destAcc.currencyCode() != null && !destAcc.currencyCode().equalsIgnoreCase(request.currency())) {
                throw new IllegalArgumentException("Destination account currency (" + destAcc.currencyCode() + ") does not match transfer currency (" + request.currency() + ")");
            }
        } else if (request.destinationAccountId() != null) {
            destAcc = verifyDestinationAccount(request.destinationAccountId(), request.currency());
        } else {
            throw new IllegalArgumentException("Beneficiary account number is required");
        }

        // 4. Resolve source account
        AccountServiceClient.AccountResponse sourceAcc;
        if (request.sourceAccountNumber() != null && !request.sourceAccountNumber().isBlank()) {
            try {
                sourceAcc = accountServiceClient.getByAccountNumber(request.sourceAccountNumber().trim().toUpperCase());
            } catch (Exception ex) {
                throw new ResourceNotFoundException("Source account number not found: " + request.sourceAccountNumber());
            }
            if (sourceAcc == null || sourceAcc.accountId() == null) {
                throw new ResourceNotFoundException("Source account number not found: " + request.sourceAccountNumber());
            }
            if (sourceAcc.customerId() != null && !sourceAcc.customerId().equalsIgnoreCase(customerId)) {
                throw new SecurityException("Unauthorized: Source account does not belong to customer " + customerId);
            }
            if (sourceAcc.accountStatus() != null && !"ACTIVE".equalsIgnoreCase(sourceAcc.accountStatus())) {
                throw new IllegalStateException("Source account is not ACTIVE (Status: " + sourceAcc.accountStatus() + ")");
            }
            if (sourceAcc.currencyCode() != null && !sourceAcc.currencyCode().equalsIgnoreCase(request.currency())) {
                throw new IllegalArgumentException("Source account currency (" + sourceAcc.currencyCode() + ") does not match transfer currency (" + request.currency() + ")");
            }
        } else if (request.sourceAccountId() != null) {
            sourceAcc = verifySourceAccount(request.sourceAccountId(), customerId, request.currency());
        } else {
            throw new IllegalArgumentException("Source account is required");
        }

        if (sourceAcc.accountId().equals(destAcc.accountId())) {
            throw new IllegalArgumentException("Source and destination accounts cannot be identical");
        }

        // If sender email is not provided in headers, attempt to resolve from auth-service
        if (userEmail == null || userEmail.isBlank()) {
            try {
                AuthServiceClient.UserSummaryResponse senderSummary = authServiceClient.getUserByCustomerId(customerId);
                if (senderSummary != null && senderSummary.email() != null && !senderSummary.email().isBlank()) {
                    userEmail = senderSummary.email().trim();
                }
            } catch (Exception ex) {
                log.warn("Could not resolve sender email from auth-service for customerId {}: {}", customerId, ex.getMessage());
            }
        }

        // 5. Pre-flight verification: customer status check and KYC compliance
        verifyCustomerActive(customerId);
        verifyCustomerKyc(customerId, "Sender");
        if (destAcc != null && destAcc.customerId() != null && !destAcc.customerId().isBlank()) {
            verifyCustomerKyc(destAcc.customerId(), "Beneficiary");
        }

        // 6. Determine and normalize types for database constraints
        String normalizedMode = "SCHEDULED".equalsIgnoreCase(request.transferMode()) ? "SCHEDULED" : "IMMEDIATE";
        String normalizedType = determineTransactionType(request.transferType(), sourceAcc, destAcc);

        // 7. Idempotency management
        String requestHash = idempotencyService.computeHash(
                sourceAcc.accountId() + ":" + destAcc.accountId() + ":" +
                request.amount() + ":" + request.currency() + ":" + normalizedMode
        );

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            IdempotencyRecord existing = idempotencyService.find(idempotencyKey);
            if (existing != null) {
                if (existing.getRequestHash() != null && !existing.getRequestHash().equals(requestHash)) {
                    throw new DuplicateRequestException("Idempotency key was previously used with different transfer parameters");
                }
                if (existing.getTransaction() != null) {
                    log.info("Idempotent request recognized for key: {}. Returning existing txn: {}",
                            idempotencyKey, existing.getTransaction().getTransactionReference());
                    return toResponse(existing.getTransaction());
                }
                throw new DuplicateRequestException("Idempotency key is currently being processed");
            }
            idempotencyService.createProcessingRecord(idempotencyKey, customerId, requestHash);
        }

        // 8. Save initial Transaction record
        Transaction transaction = initializeTransaction(request, sourceAcc.accountId(), destAcc.accountId(), customerId, initiatedBy, normalizedType);
        saveTransferDetails(transaction, normalizedType, normalizedMode, request.description(),
                "SCHEDULED".equals(normalizedMode) ? request.scheduledAt() : null);

        // 7. Process based on mode
        if ("SCHEDULED".equals(normalizedMode)) {
            if (request.scheduledAt() == null || request.scheduledAt().isBefore(LocalDateTime.now())) {
                fail(transaction, "Scheduled time must be set in the future", userEmail, idempotencyKey);
                throw new IllegalArgumentException("Scheduled time must be set in the future");
            }
            transaction.setTransactionStatus("PROCESSING");
            transaction.setUpdatedAt(LocalDateTime.now());
            transaction = transactionRepository.save(transaction);
            recordStatus(transaction, "INITIATED", "PROCESSING", "Scheduled transfer accepted");
            eventProducer.publishTransactionEvent(transaction, userEmail);
            return toResponse(transaction);
        }

        return executeImmediateTransfer(transaction, userEmail, idempotencyKey);
    }

    public TransactionResponse executeImmediateTransfer(Transaction transaction, String userEmail, String idempotencyKey) {
        verifyCustomerKyc(transaction.getCustomerId(), "Sender");

        transaction.setTransactionStatus("PROCESSING");
        transaction.setUpdatedAt(LocalDateTime.now());
        transaction = transactionRepository.save(transaction);
        recordStatus(transaction, "INITIATED", "PROCESSING", "Transfer processing started");

        // Resolve sender email if missing
        String senderEmail = userEmail;
        if (senderEmail == null || senderEmail.isBlank()) {
            try {
                AuthServiceClient.UserSummaryResponse senderSummary = authServiceClient.getUserByCustomerId(transaction.getCustomerId());
                if (senderSummary != null && senderSummary.email() != null && !senderSummary.email().isBlank()) {
                    senderEmail = senderSummary.email().trim();
                }
            } catch (Exception ex) {
                log.warn("Could not retrieve sender email from auth-service for customerId {}: {}", transaction.getCustomerId(), ex.getMessage());
            }
        }

        AccountServiceClient.BalanceOperationRequest operation =
                new AccountServiceClient.BalanceOperationRequest(
                        transaction.getAmount(),
                        transaction.getTransactionReference(),
                        transaction.getDescription() != null ? transaction.getDescription() : "Fund Transfer",
                        transaction.getInitiatedBy()
                );

        // Step A: Debit Source Account
        boolean debitSucceeded = false;
        try {
            AccountServiceClient.BalanceOperationResponse debit =
                    accountServiceClient.debit(transaction.getSourceAccountId(), operation);

            if (debit != null && debit.successful()) {
                debitSucceeded = true;
            } else {
                String reason = debit != null ? debit.message() : "Debit operation rejected by account service";
                fail(transaction, reason, senderEmail, idempotencyKey);
                return toResponse(transaction);
            }
        } catch (FeignException ex) {
            String errorMsg = extractFeignErrorMessage(ex);
            fail(transaction, "Debit failed: " + errorMsg, senderEmail, idempotencyKey);
            return toResponse(transaction);
        } catch (Exception ex) {
            fail(transaction, "Debit failed: " + ex.getMessage(), senderEmail, idempotencyKey);
            return toResponse(transaction);
        }

        // Step B: Credit Destination Account with Automated Compensating Refund on failure
        try {
            AccountServiceClient.BalanceOperationResponse credit =
                    accountServiceClient.credit(transaction.getDestinationAccountId(), operation);

            if (credit == null || !credit.successful()) {
                String reason = credit != null ? credit.message() : "Credit operation rejected";
                compensateAndFail(transaction, reason, senderEmail, idempotencyKey);
                return toResponse(transaction);
            }

            // Success lifecycle
            transaction.setTransactionStatus("SUCCESS");
            transaction.setCompletedAt(LocalDateTime.now());
            transaction.setUpdatedAt(LocalDateTime.now());
            transaction = transactionRepository.save(transaction);
            recordStatus(transaction, "PROCESSING", "SUCCESS", "Transfer completed successfully");

            // Dispatch Debit alert to sender
            eventProducer.publishTransactionEvent(transaction, senderEmail);

            // Notify Receiver with original registered email
            try {
                if (transaction.getDestinationAccountId() != null) {
                    AccountServiceClient.AccountResponse destAcc = accountServiceClient.getAccount(transaction.getDestinationAccountId());
                    if (destAcc != null && destAcc.customerId() != null) {
                        String receiverEmail = null;

                        // Case 1: Self-transfer (receiver is same customer as sender)
                        if (destAcc.customerId().equalsIgnoreCase(transaction.getCustomerId()) && senderEmail != null && !senderEmail.isBlank()) {
                            receiverEmail = senderEmail;
                        } else {
                            // Case 2: Transfer to another customer - look up registered email from auth-service
                            try {
                                AuthServiceClient.UserSummaryResponse userSummary = authServiceClient.getUserByCustomerId(destAcc.customerId());
                                if (userSummary != null && userSummary.email() != null && !userSummary.email().isBlank()) {
                                    receiverEmail = userSummary.email().trim();
                                }
                            } catch (Exception authEx) {
                                log.warn("Could not retrieve original email from auth-service for receiver customerId {}: {}",
                                        destAcc.customerId(), authEx.getMessage());
                            }
                        }

                        if (receiverEmail != null && !receiverEmail.isBlank()) {
                            log.info("Dispatching credit alert email to receiver [{}] (customerId: {}) for txn {}",
                                    receiverEmail, destAcc.customerId(), transaction.getTransactionReference());
                            eventProducer.publishReceiverTransactionEvent(transaction, destAcc.customerId(), receiverEmail);
                        } else {
                            log.warn("Receiver email could not be resolved for customerId {}. Skipping credit notification.", destAcc.customerId());
                        }
                    }
                }
            } catch (Exception rcvEx) {
                log.warn("Could not dispatch receiver transaction notification: {}", rcvEx.getMessage());
            }

            completeIdempotency(idempotencyKey, transaction, "COMPLETED");
            return toResponse(transaction);

        } catch (Exception ex) {
            String reason = ex instanceof FeignException feignEx ? extractFeignErrorMessage(feignEx) : ex.getMessage();
            compensateAndFail(transaction, reason, senderEmail, idempotencyKey);
            return toResponse(transaction);
        }
    }

    /**
     * Automatic Compensation (Saga Pattern Reversal):
     * If source account was debited but destination credit failed, refund source account immediately.
     */
    private void compensateAndFail(Transaction transaction, String failureReason, String userEmail, String idempotencyKey) {
        log.warn("Destination credit failed for txn {}. Initiating automatic compensating refund on source account {}",
                transaction.getTransactionReference(), transaction.getSourceAccountId());

        try {
            AccountServiceClient.BalanceOperationRequest refundRequest =
                    new AccountServiceClient.BalanceOperationRequest(
                            transaction.getAmount(),
                            transaction.getTransactionReference() + "-REVERSAL",
                            "Automatic refund for failed transfer " + transaction.getTransactionReference(),
                            "SYSTEM"
                    );

            AccountServiceClient.BalanceOperationResponse refund =
                    accountServiceClient.credit(transaction.getSourceAccountId(), refundRequest);

            if (refund != null && refund.successful()) {
                log.info("Compensating refund successfully processed for txn {}", transaction.getTransactionReference());
                recordStatus(transaction, "PROCESSING", "REVERSED", "Credit failed; source account refunded automatically");
                fail(transaction, "Credit failed: " + failureReason + " (Sender account refunded)", userEmail, idempotencyKey);
            } else {
                log.error("CRITICAL: Compensating refund failed for txn {}!", transaction.getTransactionReference());
                fail(transaction, "Credit failed: " + failureReason + " (Refund pending manual reconciliation)", userEmail, idempotencyKey);
            }
        } catch (Exception refundEx) {
            log.error("CRITICAL: Exception occurred while refunding source account for txn {}: {}",
                    transaction.getTransactionReference(), refundEx.getMessage(), refundEx);
            fail(transaction, "Credit failed: " + failureReason + " (Refund exception: " + refundEx.getMessage() + ")", userEmail, idempotencyKey);
        }
    }

    @Transactional(readOnly = true)
    public TransactionResponse getByReference(String reference) {
        return transactionRepository.findByTransactionReference(reference)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + reference));
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsByCustomer(String customerId) {
        return transactionRepository.findByCustomerIdOrderByInitiatedAtDesc(customerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionsByAccount(Long accountId) {
        return transactionRepository.findBySourceAccountIdOrDestinationAccountIdOrderByInitiatedAtDesc(accountId, accountId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void verifyCustomerActive(String customerId) {
        try {
            UserServiceClient.CustomerResponse customer = userServiceClient.getCustomer(customerId);
            if (customer != null && customer.customerStatus() != null) {
                String status = customer.customerStatus();
                if ("BLOCKED".equalsIgnoreCase(status) || "REJECTED".equalsIgnoreCase(status) ||
                        "FROZEN".equalsIgnoreCase(status) || "DISABLED".equalsIgnoreCase(status) ||
                        "INACTIVE".equalsIgnoreCase(status)) {
                    throw new IllegalStateException("Customer account is blocked (Status: " + status + ")");
                }
            }
        } catch (FeignException.NotFound ex) {
            log.warn("Customer {} not found in User-Service; proceeding with edge token claims", customerId);
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Unable to contact User-Service for customer check: {}", ex.getMessage());
        }
    }

    private void verifyCustomerKyc(String customerId, String roleDescription) {
        if (customerId == null || customerId.isBlank()) {
            return;
        }
        try {
            // 1. Check via dedicated KYC status endpoint
            UserServiceClient.CustomerKycResponse kyc = null;
            try {
                kyc = userServiceClient.getKycStatus(customerId.trim());
            } catch (Exception ex) {
                log.debug("Direct KYC endpoint check error for {}: {}, fallback to getCustomer", customerId, ex.getMessage());
            }

            if (kyc != null) {
                if (!kyc.kycCompleted()) {
                    throw new IllegalStateException(
                            "KYC Verification Required: " + roleDescription + " (" + customerId +
                            ") has not completed customer profile setup. Transfers are prohibited until KYC is completed."
                    );
                }
                return;
            }

            // 2. Fallback to customer entity
            UserServiceClient.CustomerResponse customer = userServiceClient.getCustomer(customerId.trim());
            if (customer != null) {
                boolean isDone = Boolean.TRUE.equals(customer.kycCompleted()) ||
                        "COMPLETED".equalsIgnoreCase(customer.kycStatus()) ||
                        "VERIFIED".equalsIgnoreCase(customer.kycStatus());
                if (!isDone) {
                    throw new IllegalStateException(
                            "KYC Verification Required: " + roleDescription + " (" + customerId +
                            ") has not completed customer profile setup. Transfers are prohibited until KYC is completed."
                    );
                }
            } else {
                throw new IllegalStateException("Customer profile not found for " + roleDescription + " (" + customerId + "). KYC profile setup is compulsory for transfers.");
            }
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (feign.FeignException.NotFound ex) {
            throw new IllegalStateException("Customer profile not found for " + roleDescription + " (" + customerId + "). KYC profile setup is compulsory for transfers.");
        } catch (Exception ex) {
            log.error("Unable to verify KYC compliance for {} ({}): {}", roleDescription, customerId, ex.getMessage());
            throw new IllegalStateException("Unable to verify KYC compliance for " + roleDescription + " (" + customerId + "). Transfer blocked for security.");
        }
    }

    private AccountServiceClient.AccountResponse verifySourceAccount(Long accountId, String customerId, String currency) {
        try {
            AccountServiceClient.AccountResponse account = accountServiceClient.getAccount(accountId);
            if (account == null) {
                throw new ResourceNotFoundException("Source account " + accountId + " not found");
            }
            if (account.customerId() != null && !account.customerId().equalsIgnoreCase(customerId)) {
                throw new SecurityException("Unauthorized: Source account " + accountId + " does not belong to customer " + customerId);
            }
            if (account.accountStatus() != null && !"ACTIVE".equalsIgnoreCase(account.accountStatus())) {
                throw new IllegalStateException("Source account " + accountId + " is not ACTIVE (Status: " + account.accountStatus() + ")");
            }
            if (account.currencyCode() != null && !account.currencyCode().equalsIgnoreCase(currency)) {
                throw new IllegalArgumentException("Source account currency (" + account.currencyCode() + ") does not match transfer currency (" + currency + ")");
            }
            return account;
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Source account " + accountId + " not found in Account Service");
        }
    }

    private AccountServiceClient.AccountResponse verifyDestinationAccount(Long accountId, String currency) {
        try {
            AccountServiceClient.AccountResponse account = accountServiceClient.getAccount(accountId);
            if (account == null) {
                throw new ResourceNotFoundException("Destination account " + accountId + " not found");
            }
            if (account.accountStatus() != null && !"ACTIVE".equalsIgnoreCase(account.accountStatus())) {
                throw new IllegalStateException("Destination account " + accountId + " is not ACTIVE (Status: " + account.accountStatus() + ")");
            }
            if (account.currencyCode() != null && !account.currencyCode().equalsIgnoreCase(currency)) {
                throw new IllegalArgumentException("Destination account currency (" + account.currencyCode() + ") does not match transfer currency (" + currency + ")");
            }
            return account;
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Destination account " + accountId + " not found in Account Service");
        }
    }

    private String determineTransactionType(String requestedType,
                                           AccountServiceClient.AccountResponse sourceAcc,
                                           AccountServiceClient.AccountResponse destAcc) {
        if (sourceAcc != null && destAcc != null &&
                sourceAcc.customerId() != null && destAcc.customerId() != null &&
                sourceAcc.customerId().equalsIgnoreCase(destAcc.customerId())) {
            return "SELF_TRANSFER";
        }
        if ("SELF_TRANSFER".equalsIgnoreCase(requestedType) || "SELF".equalsIgnoreCase(requestedType)) {
            return "SELF_TRANSFER";
        }
        if ("CUSTOMER_TRANSFER".equalsIgnoreCase(requestedType)) {
            return "CUSTOMER_TRANSFER";
        }
        return "FUND_TRANSFER";
    }

    private Transaction initializeTransaction(TransferRequest request, Long sourceAccountId, Long destinationAccountId, String customerId, String initiatedBy, String txnType) {
        Transaction transaction = new Transaction();
        transaction.setTransactionReference("TXN-" + UUID.randomUUID());
        transaction.setTransactionType(txnType);
        transaction.setTransactionStatus("INITIATED");
        transaction.setSourceAccountId(sourceAccountId != null ? sourceAccountId : request.sourceAccountId());
        transaction.setDestinationAccountId(destinationAccountId != null ? destinationAccountId : request.destinationAccountId());
        transaction.setCustomerId(customerId);
        transaction.setInitiatedBy(initiatedBy);
        transaction.setAmount(request.amount());
        transaction.setCurrency(request.currency().toUpperCase());
        transaction.setDescription(request.description());
        transaction.setInitiatedAt(LocalDateTime.now());
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        transaction = transactionRepository.save(transaction);

        recordStatus(transaction, null, "INITIATED", "Transfer created");
        return transaction;
    }

    private void saveTransferDetails(Transaction transaction, String txnType, String mode, String description, LocalDateTime scheduledAt) {
        TransferDetails details = new TransferDetails();
        details.setTransaction(transaction);
        details.setTransferType(txnType);
        details.setTransferMode(mode);
        details.setRemarks(description);
        details.setScheduledAt(scheduledAt);
        details.setCreatedAt(LocalDateTime.now());
        transferDetailsRepository.save(details);
    }

    private void fail(Transaction transaction, String reason, String userEmail, String idempotencyKey) {
        transaction.setTransactionStatus("FAILED");
        transaction.setFailureReason(reason);
        transaction.setCompletedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        transaction = transactionRepository.save(transaction);
        recordStatus(transaction, "PROCESSING", "FAILED", reason);
        eventProducer.publishTransactionEvent(transaction, userEmail);
        completeIdempotency(idempotencyKey, transaction, "FAILED");
    }

    private void completeIdempotency(String key, Transaction transaction, String status) {
        if (key == null || key.isBlank()) {
            return;
        }
        IdempotencyRecord record = idempotencyService.find(key);
        if (record != null) {
            record.setTransaction(transaction);
            idempotencyService.complete(record, status);
        }
    }

    private void recordStatus(Transaction transaction, String previousStatus, String newStatus, String reason) {
        TransactionStatusHistory history = new TransactionStatusHistory();
        history.setTransaction(transaction);
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(newStatus);
        history.setReason(reason != null && reason.length() > 495 ? reason.substring(0, 492) + "..." : reason);
        history.setChangedAt(LocalDateTime.now());
        historyRepository.save(history);
    }

    private String extractFeignErrorMessage(FeignException ex) {
        String content = ex.contentUTF8();
        if (content != null && !content.isBlank()) {
            return content.length() > 300 ? content.substring(0, 297) + "..." : content;
        }
        return ex.getMessage() != null && ex.getMessage().length() > 300 ?
                ex.getMessage().substring(0, 297) + "..." : (ex.getMessage() != null ? ex.getMessage() : "Feign Error");
    }

    public TransactionResponse toResponse(Transaction t) {
        return new TransactionResponse(
                t.getTransactionId(),
                t.getTransactionReference(),
                t.getTransactionType(),
                t.getTransactionStatus(),
                t.getSourceAccountId(),
                t.getDestinationAccountId(),
                t.getCustomerId(),
                t.getAmount(),
                t.getCurrency(),
                t.getDescription(),
                t.getFailureReason(),
                t.getInitiatedAt(),
                t.getCompletedAt()
        );
    }
}
