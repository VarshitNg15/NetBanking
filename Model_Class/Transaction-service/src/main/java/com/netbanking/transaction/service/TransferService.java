package com.netbanking.transaction.service;

import com.netbanking.transaction.dto.request.TransferRequest;
import com.netbanking.transaction.dto.response.TransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransactionService transactionService;

    public TransactionResponse transfer(TransferRequest request, String customerId, String initiatedBy, String idempotencyKey) {
        return transfer(request, customerId, initiatedBy, null, idempotencyKey);
    }

    public TransactionResponse transfer(TransferRequest request, String customerId, String initiatedBy, String userEmail, String idempotencyKey) {
        return transactionService.createTransfer(request, customerId, initiatedBy, userEmail, idempotencyKey);
    }

    public TransactionResponse getByReference(String reference) {
        return transactionService.getByReference(reference);
    }

    public List<TransactionResponse> getByCustomer(String customerId) {
        return transactionService.getTransactionsByCustomer(customerId);
    }

    public List<TransactionResponse> getByAccount(Long accountId) {
        return transactionService.getTransactionsByAccount(accountId);
    }
}
