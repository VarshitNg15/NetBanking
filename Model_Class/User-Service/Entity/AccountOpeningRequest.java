package com.netbanking.userservice.entity;

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
    @Column(name = "REQUEST_ID", length = 30)
    private String requestId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "CUSTOMER_ID",
        nullable = false,
        foreignKey = @ForeignKey(name = "FK_REQUEST_CUSTOMER")
    )
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "REQUEST_STATUS", nullable = false, length = 20)
    private RequestStatus requestStatus;

    @Column(name = "SUBMITTED_AT", nullable = false)
    private LocalDateTime submittedAt;

    /*
     * Admin identifier.
     * Auth/User service owns the actual admin identity.
     */
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

    /*
     * A request can contain SAVINGS, CURRENT,
     * or both account types.
     */
    @OneToMany(
        mappedBy = "accountOpeningRequest",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @Builder.Default
    private List<AccountOpeningRequestType> requestedAccountTypes =
            new ArrayList<>();
}