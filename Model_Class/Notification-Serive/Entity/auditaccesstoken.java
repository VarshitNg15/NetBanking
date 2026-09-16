package com.netbanking.notificationservice.model;

import com.netbanking.notificationservice.model.enums.AuditAccessTokenStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "AUDIT_ACCESS_TOKENS",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "UQ_AUDIT_ACCESS_TOKENS_HASH",
            columnNames = "TOKEN_HASH"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditAccessToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TOKEN_ID")
    private Long tokenId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "REQUEST_ID",
        nullable = false,
        foreignKey = @ForeignKey(
            name = "FK_AUDIT_ACCESS_TOKENS_REQUEST"
        )
    )
    private AuditAccessRequest accessRequest;

    /**
     * Never store the plaintext passkey.
     */
    @Column(name = "TOKEN_HASH", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Column(name = "ISSUED_AT", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "EXPIRES_AT", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "USED_AT")
    private LocalDateTime usedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    @Builder.Default
    private AuditAccessTokenStatus status =
            AuditAccessTokenStatus.ACTIVE;
}