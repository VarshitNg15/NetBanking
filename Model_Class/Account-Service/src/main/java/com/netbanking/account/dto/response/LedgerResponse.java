package com.netbanking.account.dto.response;

import com.netbanking.account.entity.AccountLedger;
import com.netbanking.account.entity.EntryType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record LedgerResponse(
    Long id,
    String transactionReference,
    String entryReference,
    EntryType entryType,
    BigDecimal amount,
    BigDecimal balanceBefore,
    BigDecimal balanceAfter,
    BigDecimal availableBalanceBefore,
    BigDecimal availableBalanceAfter,
    String description,
    String createdBy,
    LocalDateTime createdAt
) {
    public static LedgerResponse from(AccountLedger l) {
        return new LedgerResponse(
            l.getId(),
            l.getTransactionReference(),
            l.getEntryReference(),
            l.getEntryType(),
            l.getAmount(),
            l.getBalanceBefore(),
            l.getBalanceAfter(),
            l.getAvailableBalanceBefore(),
            l.getAvailableBalanceAfter(),
            l.getDescription(),
            l.getCreatedBy(),
            l.getCreatedAt()
        );
    }
}
