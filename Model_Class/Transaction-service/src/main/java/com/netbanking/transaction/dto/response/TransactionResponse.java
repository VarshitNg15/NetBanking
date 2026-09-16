package com.netbanking.transaction.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long transactionId,
        String transactionReference,
        String transactionType,
        String transactionStatus,
        Long sourceAccountId,
        Long destinationAccountId,
        String customerId,
        BigDecimal amount,
        String currency,
        String description,
        String failureReason,
        LocalDateTime initiatedAt,
        LocalDateTime completedAt
) {}
