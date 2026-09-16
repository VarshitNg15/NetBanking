package com.netbanking.transaction.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "TRANSACTIONS",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "UQ_TXN_REFERENCE",
            columnNames = "TRANSACTION_REFERENCE"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
public class Transaction {

    @Id
    @Column(name = "TRANSACTION_ID")
    private Long transactionId;

    @Column(
        name = "TRANSACTION_REFERENCE",
        nullable = false,
        unique = true,
        length = 100
    )
    private String transactionReference;

    @Column(name = "TRANSACTION_TYPE", nullable = false, length = 30)
    private String transactionType;

    @Column(name = "TRANSACTION_STATUS", nullable = false, length = 20)
    private String transactionStatus;

    /*
     * Logical references to Account Service.
     * Do NOT use @ManyToOne because Account Service
     * owns the ACCOUNT table in another microservice.
     */
    @Column(name = "SOURCE_ACCOUNT_ID", nullable = false)
    private Long sourceAccountId;

    @Column(name = "DESTINATION_ACCOUNT_ID", nullable = false)
    private Long destinationAccountId;

    /*
     * Logical reference to User Service.
     */
    @Column(name = "CUSTOMER_ID", nullable = false, length = 50)
    private String customerId;

    /*
     * Actor/token identity.
     */
    @Column(name = "INITIATED_BY", nullable = false, length = 50)
    private String initiatedBy;

    @Column(name = "AMOUNT", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "CURRENCY", nullable = false, length = 3)
    private String currency;

    @Column(name = "DESCRIPTION", length = 255)
    private String description;

    @Column(name = "FAILURE_REASON", length = 500)
    private String failureReason;

    @Column(name = "INITIATED_AT", nullable = false)
    private LocalDateTime initiatedAt;

    @Column(name = "COMPLETED_AT")
    private LocalDateTime completedAt;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;
}