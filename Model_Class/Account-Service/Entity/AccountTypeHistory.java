package com.netbanking.accountservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ACCOUNT_TYPE_HISTORY")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountTypeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "HISTORY_ID")
    private Long historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ACCOUNT_ID", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "OLD_ACCOUNT_TYPE", nullable = false, length = 20)
    private AccountType oldAccountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "NEW_ACCOUNT_TYPE", nullable = false, length = 20)
    private AccountType newAccountType;

    @Column(name = "CHANGED_BY", nullable = false, length = 100)
    private String changedBy;

    @Column(name = "REASON", length = 500)
    private String reason;

    @Column(name = "CHANGED_AT", nullable = false)
    private LocalDateTime changedAt;
}