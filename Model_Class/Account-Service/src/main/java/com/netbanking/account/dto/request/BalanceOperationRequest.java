package com.netbanking.account.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record BalanceOperationRequest(
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotBlank String transactionReference,
        String description,
        String initiatedBy
) {}
