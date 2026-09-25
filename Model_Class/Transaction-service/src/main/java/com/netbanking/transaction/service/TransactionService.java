package com.netbanking.transaction.service;

import com.netbanking.transaction.client.AccountServiceClient;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransferDetailsRepository transferDetailsRepository;
    private final TransactionStatusHistoryRepository historyRepository;
    private final IdempotencyService idempotencyService;
    private final AccountServiceClient accountServiceClient;
    private final UserServiceClient userServiceClient;
    private final TransactionEventProducer eventProducer;

    public TransactionResponse createTransfer(TransferRequest request, String customerId, String initiatedBy, String idempotencyKey) {
        return createTransfer(request, customerId, initiatedBy, null, idempotencyKey);
    }

    public TransactionResponse createTransfer(TransferRequest request, String customerId, String initiatedBy, String userEmail, String idempotencyKey) {
        // 1. Basic validation
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer ID header (X-Customer-Id) is required");
        }
        if (request.sourceAccountId().equals(request.destinationAccountId())) {
            throw new IllegalArgumentException("Source and destination accounts cannot be identical");
        }
        if (initiatedBy == null || initiatedBy.isBlank()) {
            initiatedBy = "CUSTOMER";
        }

        // 2. Pre-flight verification: customer status check
        verifyCustomerActive(customerId);

        // 3. Pre-flight verification: source & destination accounts
        AccountServiceClient.AccountResponse sourceAcc = verifySourceAccount(request.sourceAccountId(), customerId, request.currency());
        AccountServiceClient.AccountResponse destAcc = verifyDestinationAccount(request.destinationAccountId(), request.currency());

        // 4. Determine and normalize types for database constraints
        String normalizedMode = "SCHEDULED".equalsIgnoreCase(request.transferMode()) ? "SCHEDULED" : "IMMEDIATE";
        String normalizedType = determineTransactionType(request.transferType(), sourceAcc, destAcc);

        // 5. Idempotency management
        String requestHash = idempotencyService.computeHash(
                request.sourceAccountId() + ":" + request.destinationAccountId() + ":" +
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

        // 6. Save initial Transaction record
        Transaction transaction = initializeTransaction(request, customerId, initiatedBy, normalizedType);
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
        transaction.setTransactionStatus("PROCESSING");
        transaction.setUpdatedAt(LocalDateTime.now());
        transaction = transactionRepository.save(transaction);
        recordStatus(transaction, "INITIATED", "PROCESSING", "Transfer processing started");

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
                fail(transaction, reason, userEmail, idempotencyKey);
                return toResponse(transaction);
            }
        } catch (FeignException ex) {
            String errorMsg = extractFeignErrorMessage(ex);
            fail(transaction, "Debit failed: " + errorMsg, userEmail, idempotencyKey);
            return toResponse(transaction);
        } catch (Exception ex) {
            fail(transaction, "Debit failed: " + ex.getMessage(), userEmail, idempotencyKey);
            return toResponse(transaction);
        }

        // Step B: Credit Destination Account with Automated Compensating Refund on failure
        try {
            AccountServiceClient.BalanceOperationResponse credit =
                    accountServiceClient.credit(transaction.getDestinationAccountId(), operation);

            if (credit == null || !credit.successful()) {
                String reason = credit != null ? credit.message() : "Credit operation rejected";
                compensateAndFail(transaction, reason, userEmail, idempotencyKey);
                return toResponse(transaction);
            }

            // Success lifecycle
            transaction.setTransactionStatus("SUCCESS");
            transaction.setCompletedAt(LocalDateTime.now());
            transaction.setUpdatedAt(LocalDateTime.now());
            transaction = transactionRepository.save(transaction);
            recordStatus(transaction, "PROCESSING", "SUCCESS", "Transfer completed successfully");

            eventProducer.publishTransactionEvent(transaction, userEmail);

            // Notify Receiver as well (Req 13)
            try {
                if (transaction.getDestinationAccountId() != null) {
                    AccountServiceClient.AccountResponse destAcc = accountServiceClient.getAccount(transaction.getDestinationAccountId());
                    if (destAcc != null && destAcc.customerId() != null) {
                        String receiverEmail = destAcc.customerId() + "@netbank.com";
                        eventProducer.publishReceiverTransactionEvent(transaction, destAcc.customerId(), receiverEmail);
                    }
                }
            } catch (Exception rcvEx) {
                log.warn("Could not dispatch receiver transaction notification: {}", rcvEx.getMessage());
            }

            completeIdempotency(idempotencyKey, transaction, "COMPLETED");
            return toResponse(transaction);

        } catch (Exception ex) {
            String reason = ex instanceof FeignException feignEx ? extractFeignErrorMessage(feignEx) : ex.getMessage();
            compensateAndFail(transaction, reason, userEmail, idempotencyKey);
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
            if (customer != null && customer.customerStatus() != null &&
                    !"ACTIVE".equalsIgnoreCase(customer.customerStatus())) {
                throw new IllegalStateException("Customer account is not active (Status: " + customer.customerStatus() + ")");
            }
        } catch (FeignException.NotFound ex) {
            log.warn("Customer {} not found in User-Service; proceeding with edge token claims", customerId);
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Unable to contact User-Service for customer check: {}", ex.getMessage());
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

    private Transaction initializeTransaction(TransferRequest request, String customerId, String initiatedBy, String txnType) {
        Transaction transaction = new Transaction();
        transaction.setTransactionReference("TXN-" + UUID.randomUUID());
        transaction.setTransactionType(txnType);
        transaction.setTransactionStatus("INITIATED");
        transaction.setSourceAccountId(request.sourceAccountId());
        transaction.setDestinationAccountId(request.destinationAccountId());
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
