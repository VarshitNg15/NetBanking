package com.netbanking.transaction.dto.response;

import java.time.LocalDateTime;

public record StatementResponse(
        Long requestId,
        Long accountId,
        String customerId,
        String requestType,
        String status,
        String fileName,
        String fileUrl,
        LocalDateTime requestedAt,
        LocalDateTime completedAt,
        String failureReason
) {}
