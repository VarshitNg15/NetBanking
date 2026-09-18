package com.netbanking.transaction.service;

import com.netbanking.transaction.client.AccountServiceClient;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final TransferDetailsRepository transferDetailsRepository;
    private final TransactionStatusHistoryRepository historyRepository;
    private final IdempotencyService idempotencyService;
    private final AccountServiceClient accountServiceClient;
    private final TransactionEventProducer eventProducer;

    @Transactional
    public TransactionResponse createTransfer(TransferRequest request, String customerId, String initiatedBy, String idempotencyKey) {
        return createTransfer(request, customerId, initiatedBy, null, idempotencyKey);
    }

    @Transactional
    public TransactionResponse createTransfer(TransferRequest request, String customerId, String initiatedBy, String userEmail, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            IdempotencyRecord existing = idempotencyService.find(idempotencyKey);
            if (existing != null) {
                if (existing.getTransaction() != null) {
                    return toResponse(existing.getTransaction());
                }
                throw new DuplicateRequestException("Idempotency key is already being processed");
            }
            idempotencyService.createProcessingRecord(idempotencyKey, customerId, null);
        }

        Transaction transaction = new Transaction();
        transaction.setTransactionReference("TXN-" + UUID.randomUUID());
        transaction.setTransactionType(request.transferType());
        transaction.setTransactionStatus("INITIATED");
        transaction.setSourceAccountId(request.sourceAccountId());
        transaction.setDestinationAccountId(request.destinationAccountId());
        transaction.setCustomerId(customerId);
        transaction.setInitiatedBy(initiatedBy);
        transaction.setAmount(request.amount());
        transaction.setCurrency(request.currency());
        transaction.setDescription(request.description());
        transaction.setInitiatedAt(LocalDateTime.now());
        transaction.setCreatedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        transaction = transactionRepository.save(transaction);

        recordStatus(transaction, null, "INITIATED", "Transfer created");

        TransferDetails details = new TransferDetails();
        details.setTransaction(transaction);
        details.setTransferType(request.transferType());
        details.setTransferMode(request.transferMode());
        details.setRemarks(request.description());
        details.setScheduledAt(request.scheduledAt());
        details.setCreatedAt(LocalDateTime.now());
        transferDetailsRepository.save(details);

        if ("SCHEDULED".equalsIgnoreCase(request.transferMode())) {
            transaction.setTransactionStatus("PROCESSING");
            transaction.setUpdatedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
            recordStatus(transaction, "INITIATED", "PROCESSING", "Scheduled transfer accepted");
            eventProducer.publishTransactionEvent(transaction, userEmail);
            return toResponse(transaction);
        }

        return executeImmediateTransfer(transaction, userEmail, idempotencyKey);
    }

    @Transactional
    protected TransactionResponse executeImmediateTransfer(Transaction transaction, String userEmail, String idempotencyKey) {
        transaction.setTransactionStatus("PROCESSING");
        transaction.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(transaction);
        recordStatus(transaction, "INITIATED", "PROCESSING", "Transfer processing started");

        try {
            AccountServiceClient.BalanceOperationRequest operation =
                    new AccountServiceClient.BalanceOperationRequest(
                            transaction.getAmount(),
                            transaction.getTransactionReference(),
                            transaction.getDescription(),
                            transaction.getInitiatedBy()
                    );

            AccountServiceClient.BalanceOperationResponse debit =
                    accountServiceClient.debit(transaction.getSourceAccountId(), operation);

            if (!debit.successful()) {
                fail(transaction, debit.message(), userEmail, idempotencyKey);
                return toResponse(transaction);
            }

            AccountServiceClient.BalanceOperationResponse credit =
                    accountServiceClient.credit(transaction.getDestinationAccountId(), operation);

            if (!credit.successful()) {
                // A production implementation should invoke a compensating credit
                // or reversal workflow in Account Service here.
                fail(transaction, credit.message(), userEmail, idempotencyKey);
                return toResponse(transaction);
            }

            transaction.setTransactionStatus("SUCCESS");
            transaction.setCompletedAt(LocalDateTime.now());
            transaction.setUpdatedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
            recordStatus(transaction, "PROCESSING", "SUCCESS", "Transfer completed successfully");

            eventProducer.publishTransactionEvent(transaction, userEmail);
            completeIdempotency(idempotencyKey, transaction, "COMPLETED");
            return toResponse(transaction);
        } catch (RuntimeException ex) {
            fail(transaction, ex.getMessage(), userEmail, idempotencyKey);
            return toResponse(transaction);
        }
    }

    @Transactional(readOnly = true)
    public TransactionResponse getByReference(String reference) {
        return transactionRepository.findByTransactionReference(reference)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + reference));
    }

    private void fail(Transaction transaction, String reason, String userEmail, String idempotencyKey) {
        transaction.setTransactionStatus("FAILED");
        transaction.setFailureReason(reason);
        transaction.setCompletedAt(LocalDateTime.now());
        transaction.setUpdatedAt(LocalDateTime.now());
        transactionRepository.save(transaction);
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
        history.setReason(reason);
        history.setChangedAt(LocalDateTime.now());
        historyRepository.save(history);
    }

    private TransactionResponse toResponse(Transaction t) {
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
