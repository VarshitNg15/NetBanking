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
    @SequenceGenerator(name = "statement_request_seq", sequenceName = "SEQ_STATEMENT_REQUEST_ID", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "statement_request_seq")
    @Column(name = "REQUEST_ID", nullable = false)
    private Long requestId;

    @Column(name = "ACCOUNT_ID", nullable = false)
    private Long accountId;

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

    public void setFailureReason(String failureReason) {
        if (failureReason != null && failureReason.length() > 495) {
            this.failureReason = failureReason.substring(0, 492) + "...";
        } else {
            this.failureReason = failureReason;
        }
    }
}
