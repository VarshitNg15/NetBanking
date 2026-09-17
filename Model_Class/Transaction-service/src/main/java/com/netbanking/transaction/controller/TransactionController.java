package com.netbanking.transaction.controller;

import com.netbanking.transaction.dto.request.TransferRequest;
import com.netbanking.transaction.dto.response.TransactionResponse;
import com.netbanking.transaction.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/transactions", "/api/transactions"})
@RequiredArgsConstructor
public class TransactionController {

    private final TransferService transferService;

    @PostMapping({"/transfers", "/transfer"})
    public ResponseEntity<TransactionResponse> createTransfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "X-Customer-Id") String customerId,
            @RequestHeader(value = "X-Initiated-By") String initiatedBy,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        return ResponseEntity.ok(
                transferService.transfer(request, customerId, initiatedBy, idempotencyKey)
        );
    }
}
