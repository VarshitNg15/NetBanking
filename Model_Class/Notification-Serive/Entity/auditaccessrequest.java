package com.netbanking.notificationservice.model;

import com.netbanking.notificationservice.model.enums.AuditAccessRequestStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "AUDIT_ACCESS_REQUESTS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditAccessRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "REQUEST_ID")
    private Long requestId;

    /**
     * Logical reference to the Admin identity.
     */
    @Column(name = "ADMIN_ID", nullable = false, length = 50)
    private String adminId;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    @Builder.Default
    private AuditAccessRequestStatus status =
            AuditAccessRequestStatus.PENDING;

    @Column(name = "REQUESTED_AT", nullable = false)
    private LocalDateTime requestedAt;

    /**
     * Logical reference to the audit team officer.
     */
    @Column(name = "APPROVED_BY", length = 50)
    private String approvedBy;

    @Column(name = "APPROVED_AT")
    private LocalDateTime approvedAt;

    @Column(name = "EXPIRES_AT")
    private LocalDateTime expiresAt;

    @Column(name = "REASON", length = 500)
    private String reason;

    @OneToMany(
        mappedBy = "accessRequest",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @Builder.Default
    private List<AuditAccessToken> tokens = new ArrayList<>();
}