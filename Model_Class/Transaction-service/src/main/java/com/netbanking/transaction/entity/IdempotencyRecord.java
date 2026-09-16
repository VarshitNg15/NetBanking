package com.netbanking.transaction.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "IDEMPOTENCY_RECORDS")
@Getter
@Setter
@NoArgsConstructor
public class IdempotencyRecord {

    @Id
    @SequenceGenerator(name = "idempotency_seq", sequenceName = "SEQ_IDEMPOTENCY_ID", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "idempotency_seq")
    @Column(name = "IDEMPOTENCY_ID", nullable = false)
    private Long idempotencyId;

    @Column(name = "IDEMPOTENCY_KEY", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "CUSTOMER_ID", nullable = false, length = 50)
    private String customerId;

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
