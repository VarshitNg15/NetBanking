package com.netbanking.notification.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "AUDIT_LOGS")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "AUDIT_ID")
    private Long auditId;

    @Column(name = "EVENT_ID", unique = true, length = 100)
    private String eventId;

    @Column(name = "EVENT_TYPE", nullable = false, length = 100)
    private String eventType;

    @Column(name = "ACTOR_ID", length = 50)
    private String actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ACTOR_ROLE", length = 20)
    private ActorRole actorRole;

    @Column(name = "CUSTOMER_ID", length = 50)
    private String customerId;

    @Column(name = "RESOURCE_TYPE", length = 50)
    private String resourceType;

    @Column(name = "RESOURCE_ID", length = 100)
    private String resourceId;

    @Column(name = "ACTION", nullable = false, length = 100)
    private String action;

    @Column(name = "DESCRIPTION", length = 1000)
    private String description;

    @Column(name = "IP_ADDRESS", length = 45)
    private String ipAddress;

    @Column(name = "USER_AGENT", length = 500)
    private String userAgent;

    @Column(name = "EVENT_TIMESTAMP", nullable = false)
    private LocalDateTime eventTimestamp;
}
