package com.netbanking.transaction.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferRequest(
        Long sourceAccountId,
        String sourceAccountNumber,

        Long destinationAccountId,
        String destinationAccountNumber,

        @NotNull(message = "Transfer amount is required")
        @DecimalMin(value = "0.01", message = "Transfer amount must be at least 0.01")
        BigDecimal amount,

        @NotBlank(message = "Currency is required")
        String currency,

        String description,

        @NotBlank(message = "Transfer type is required")
        String transferType,

        @NotBlank(message = "Transfer mode is required")
        String transferMode,

        LocalDateTime scheduledAt
) {
}
