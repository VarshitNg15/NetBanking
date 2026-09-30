package com.netbanking.account.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record BalanceOperationRequest(
        @NotNull @DecimalMin(value = "0.01") @DecimalMax(value = "10000000.00", message = "Amount cannot exceed ₹1,00,00,000 (1 Crore INR)") BigDecimal amount,
        @NotBlank String transactionReference,
        String description,
        String initiatedBy
) {}
