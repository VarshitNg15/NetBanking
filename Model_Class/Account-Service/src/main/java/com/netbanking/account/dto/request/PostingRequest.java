package com.netbanking.account.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record PostingRequest(
        @NotBlank @Size(max=100) String transactionReference,
        @NotBlank @Size(max=100) String entryReference,
        @NotNull @DecimalMin(value="0.01") @DecimalMax(value="10000000.00", message="Deposit amount cannot exceed ₹1,00,00,000 (1 Crore INR)") BigDecimal amount,
        @Size(max=500) String description,
        @Size(max=100) String createdBy
){}

