package com.netbanking.transaction.service;

import com.netbanking.transaction.dto.request.TransferRequest;
import com.netbanking.transaction.dto.response.TransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransferService {

    private final TransactionService transactionService;

    public TransactionResponse transfer(TransferRequest request, String customerId, String initiatedBy, String idempotencyKey) {
        return transactionService.createTransfer(request, customerId, initiatedBy, idempotencyKey);
    }
}
