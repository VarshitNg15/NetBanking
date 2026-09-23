package com.netbanking.transaction.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record StatementRequestDto(
        @NotNull(message = "Account ID is required")
        Long accountId,

        String requestType,

        LocalDateTime fromDate,

        LocalDateTime toDate
) {
    public String normalizedRequestType() {
        if (requestType == null || requestType.isBlank()) {
            return "PDF";
        }
        String upper = requestType.trim().toUpperCase();
        if ("CSV".equals(upper)) {
            return "CSV";
        }
        return "PDF";
    }
}
