package com.netbanking.accountservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ACCOUNT_LEDGER")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "LEDGER_ID")
    private Long ledgerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ACCOUNT_ID", nullable = false)
    private Account account;

    @Column(name = "TRANSACTION_REFERENCE",
            nullable = false,
            length = 100)
    private String transactionReference;

    @Column(name = "ENTRY_REFERENCE",
            nullable = false,
            unique = true,
            length = 100)
    private String entryReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "ENTRY_TYPE", nullable = false, length = 10)
    private LedgerEntryType entryType;

    @Column(name = "AMOUNT",
            nullable = false,
            precision = 19,
            scale = 2)
    private BigDecimal amount;

    @Column(name = "BALANCE_BEFORE",
            nullable = false,
            precision = 19,
            scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "BALANCE_AFTER",
            nullable = false,
            precision = 19,
            scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "AVAILABLE_BALANCE_BEFORE",
            nullable = false,
            precision = 19,
            scale = 2)
    private BigDecimal availableBalanceBefore;

    @Column(name = "AVAILABLE_BALANCE_AFTER",
            nullable = false,
            precision = 19,
            scale = 2)
    private BigDecimal availableBalanceAfter;

    @Column(name = "DESCRIPTION", length = 500)
    private String description;

    @Column(name = "CREATED_BY", length = 100)
    private String createdBy;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;
}