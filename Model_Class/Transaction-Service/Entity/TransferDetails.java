package com.netbanking.transaction.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "TRANSFER_DETAILS",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "UQ_TRANSFER_TRANSACTION",
            columnNames = "TRANSACTION_ID"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
public class TransferDetails {

    @Id
    @Column(name = "TRANSFER_ID")
    private Long transferId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "TRANSACTION_ID",
        nullable = false,
        unique = true
    )
    private Transaction transaction;

    @Column(name = "TRANSFER_TYPE", nullable = false, length = 30)
    private String transferType;

    @Column(name = "TRANSFER_MODE", nullable = false, length = 20)
    private String transferMode;

    @Column(name = "REMARKS", length = 255)
    private String remarks;

    @Column(name = "SCHEDULED_AT")
    private LocalDateTime scheduledAt;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;
}