package com.netbanking.transaction.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.netbanking.transaction.dto.request.TransferRequest;
import com.netbanking.transaction.dto.response.TransactionResponse;
import com.netbanking.transaction.service.TransferService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping({"/api/v1/transactions", "/api/transactions", "/api/v1/transfers", "/api/transfers"})
@RequiredArgsConstructor
@Tag(name = "Transactions & Transfers", description = "Endpoints for initiating immediate and scheduled fund transfers and inquiries")
public class TransactionController {

    private final TransferService transferService;

    @PostMapping({"/transfers", "/transfer", ""})
    @Operation(summary = "Create fund transfer", description = "Executes immediate or schedules future fund transfer with automated compensation and idempotency guarantees.")
    public ResponseEntity<TransactionResponse> createTransfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "X-Customer-Id") String customerId,
            @RequestHeader(value = "X-Initiated-By", required = false, defaultValue = "CUSTOMER") String initiatedBy,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        return ResponseEntity.ok(
                transferService.transfer(request, customerId, initiatedBy, userEmail, idempotencyKey)
        );
    }

    @GetMapping("/{transactionReference}")
    @Operation(summary = "Get transaction by reference", description = "Returns full lifecycle status and metadata for a transaction reference.")
    public ResponseEntity<TransactionResponse> getByReference(
            @Parameter(description = "Unique transaction reference string")
            @PathVariable String transactionReference) {
        return ResponseEntity.ok(transferService.getByReference(transactionReference));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get transactions by customer ID", description = "Returns transaction list for a given customer.")
    public ResponseEntity<List<TransactionResponse>> getByCustomer(@PathVariable String customerId) {
        return ResponseEntity.ok(transferService.getByCustomer(customerId));
    }

    @GetMapping("/account/{accountId}")
    @Operation(summary = "Get transactions by account ID", description = "Returns transaction list associated with an account ID (as source or destination).")
    public ResponseEntity<List<TransactionResponse>> getByAccount(@PathVariable Long accountId) {
        return ResponseEntity.ok(transferService.getByAccount(accountId));
    }
}
