package com.netbanking.account.dto.response;

public record BalanceOperationResponse(
        boolean successful,
        String message
) {}
