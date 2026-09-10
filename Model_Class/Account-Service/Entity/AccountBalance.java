package com.netbanking.accountservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ACCOUNT_BALANCE")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountBalance {

    @Id
    @Column(name = "ACCOUNT_ID")
    private Long accountId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "ACCOUNT_ID")
    private Account account;

    @Column(name = "CURRENT_BALANCE",
            nullable = false,
            precision = 19,
            scale = 2)
    private BigDecimal currentBalance;

    @Column(name = "AVAILABLE_BALANCE",
            nullable = false,
            precision = 19,
            scale = 2)
    private BigDecimal availableBalance;

    @Column(name = "TOTAL_CREDIT",
            nullable = false,
            precision = 19,
            scale = 2)
    private BigDecimal totalCredit;

    @Column(name = "TOTAL_DEBIT",
            nullable = false,
            precision = 19,
            scale = 2)
    private BigDecimal totalDebit;

    @Version
    @Column(name = "VERSION_NO", nullable = false)
    private Long versionNo;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;
}