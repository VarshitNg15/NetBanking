package com.netbanking.transaction.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferRequest(
        @NotNull Long sourceAccountId,
        @NotNull Long destinationAccountId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotBlank String currency,
        String description,
        @NotBlank String transferType,
        @NotBlank String transferMode,
        LocalDateTime scheduledAt
) {}
