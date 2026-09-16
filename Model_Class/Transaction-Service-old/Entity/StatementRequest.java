package com.netbanking.transaction.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "STATEMENT_REQUESTS")
@Getter
@Setter
@NoArgsConstructor
public class StatementRequest {

    @Id
    @Column(name = "REQUEST_ID")
    private Long requestId;

    /*
     * Logical reference to Account Service.
     */
    @Column(name = "ACCOUNT_ID", nullable = false)
    private Long accountId;

    /*
     * Logical reference to User Service.
     */
    @Column(name = "CUSTOMER_ID", nullable = false, length = 50)
    private String customerId;

    @Column(name = "REQUESTED_BY", nullable = false, length = 50)
    private String requestedBy;

    @Column(name = "REQUEST_TYPE", nullable = false, length = 10)
    private String requestType;

    @Column(name = "FROM_DATE")
    private LocalDateTime fromDate;

    @Column(name = "TO_DATE")
    private LocalDateTime toDate;

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status;

    @Column(name = "FILE_NAME", length = 255)
    private String fileName;

    @Column(name = "FILE_URL", length = 500)
    private String fileUrl;

    @Column(name = "REQUESTED_AT", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "COMPLETED_AT")
    private LocalDateTime completedAt;

    @Column(name = "FAILURE_REASON", length = 500)
    private String failureReason;
}