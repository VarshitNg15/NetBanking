package com.netbanking.transaction.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "TRANSACTION_STATUS_HISTORY")
@Getter
@Setter
@NoArgsConstructor
public class TransactionStatusHistory {

    @Id
    @Column(name = "HISTORY_ID")
    private Long historyId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "TRANSACTION_ID",
        nullable = false
    )
    private Transaction transaction;

    @Column(name = "PREVIOUS_STATUS", length = 20)
    private String previousStatus;

    @Column(name = "NEW_STATUS", nullable = false, length = 20)
    private String newStatus;

    @Column(name = "REASON", length = 500)
    private String reason;

    @Column(name = "CHANGED_AT", nullable = false)
    private LocalDateTime changedAt;
}