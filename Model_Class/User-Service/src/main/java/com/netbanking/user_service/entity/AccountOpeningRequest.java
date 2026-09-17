package com.netbanking.user_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ACCOUNT_OPENING_REQUEST")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountOpeningRequest {

    @Id
    @Column(name = "REQUEST_ID", length = 30, nullable = false)
    private String requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "CUSTOMER_ID",
            nullable = false
    )
    private Customer customer;

    @Column(name = "REQUEST_STATUS", length = 20, nullable = false)
    private String requestStatus;

    @Column(name = "SUBMITTED_AT", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "REVIEWED_BY", length = 50)
    private String reviewedBy;

    @Column(name = "REVIEWED_AT")
    private LocalDateTime reviewedAt;

    @Column(name = "REJECTION_REASON", length = 500)
    private String rejectionReason;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(
            mappedBy = "accountOpeningRequest",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<AccountOpeningRequestType> requestedAccountTypes =
            new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (submittedAt == null) {
            submittedAt = now;
        }

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}