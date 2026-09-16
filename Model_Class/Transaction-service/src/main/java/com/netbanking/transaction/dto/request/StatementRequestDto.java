package com.netbanking.transaction.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record StatementRequestDto(
        @NotNull Long accountId,
        @NotBlank String requestType,
        LocalDateTime fromDate,
        LocalDateTime toDate
) {}
