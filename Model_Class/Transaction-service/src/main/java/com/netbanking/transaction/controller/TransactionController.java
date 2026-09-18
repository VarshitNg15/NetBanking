package com.netbanking.transaction.controller;

import com.netbanking.transaction.dto.request.TransferRequest;
import com.netbanking.transaction.dto.response.TransactionResponse;
import com.netbanking.transaction.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/transactions", "/api/transactions"})
@RequiredArgsConstructor
@Tag(name = "Transactions & Transfers", description = "Endpoints for initiating immediate and scheduled fund transfers")
public class TransactionController {

    private final TransferService transferService;

    @PostMapping({"/transfers", "/transfer"})
    public ResponseEntity<TransactionResponse> createTransfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "X-Customer-Id") String customerId,
            @RequestHeader(value = "X-Initiated-By") String initiatedBy,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        return ResponseEntity.ok(
                transferService.transfer(request, customerId, initiatedBy, userEmail, idempotencyKey)
        );
    }
}
