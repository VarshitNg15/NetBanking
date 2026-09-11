package com.netbanking.transaction.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "IDEMPOTENCY_RECORDS",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "UQ_IDEMPOTENCY_KEY",
            columnNames = "IDEMPOTENCY_KEY"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
public class IdempotencyRecord {

    @Id
    @Column(name = "IDEMPOTENCY_ID")
    private Long idempotencyId;

    @Column(
        name = "IDEMPOTENCY_KEY",
        nullable = false,
        unique = true,
        length = 100
    )
    private String idempotencyKey;

    @Column(name = "CUSTOMER_ID", nullable = false, length = 50)
    private String customerId;

    /*
     * Optional relationship to a transaction within
     * the same Transaction Service.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TRANSACTION_ID")
    private Transaction transaction;

    @Column(name = "REQUEST_HASH", length = 128)
    private String requestHash;

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "EXPIRES_AT")
    private LocalDateTime expiresAt;
}