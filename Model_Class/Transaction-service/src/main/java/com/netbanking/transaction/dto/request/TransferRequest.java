package com.netbanking.transaction.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferRequest(
        @NotNull(message = "Source account ID is required")
        Long sourceAccountId,

        @NotNull(message = "Destination account ID is required")
        Long destinationAccountId,

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
    @AssertTrue(message = "Source and destination accounts must be different")
    public boolean isDifferentAccounts() {
        if (sourceAccountId == null || destinationAccountId == null) {
            return true;
        }
        return !sourceAccountId.equals(destinationAccountId);
    }
}
