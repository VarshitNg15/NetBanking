package com.netbanking.account.dto.response;

import com.netbanking.account.entity.Account;
import com.netbanking.account.entity.AccountStatus;
import com.netbanking.account.entity.AccountType;
import java.time.LocalDateTime;

public record AccountResponse(
        Long id,
        String customerId,
        String accountNumber,
        AccountType accountType,
        AccountStatus status,
        String currencyCode,
        LocalDateTime createdAt,
        LocalDateTime approvedAt
) {
    public Long accountId() {
        return id;
    }

    public String accountStatus() {
        return status != null ? status.name() : null;
    }

    public static AccountResponse from(Account a) {
        return new AccountResponse(
                a.getId(),
                a.getCustomerId(),
                a.getAccountNumber(),
                a.getAccountType(),
                a.getAccountStatus(),
                a.getCurrencyCode(),
                a.getCreatedAt(),
                a.getApprovedAt()
        );
    }
}
