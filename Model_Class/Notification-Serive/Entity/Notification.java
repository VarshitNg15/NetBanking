package com.netbanking.notificationservice.model;

import com.netbanking.notificationservice.model.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "NOTIFICATIONS",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "UQ_NOTIFICATIONS_EVENT_ID",
            columnNames = "EVENT_ID"
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "NOTIFICATION_ID")
    private Long notificationId;

    @Column(name = "EVENT_ID", nullable = false, unique = true, length = 100)
    private String eventId;

    @Column(name = "EVENT_TYPE", nullable = false, length = 100)
    private String eventType;

    /**
     * Logical reference to User/Auth Service.
     */
    @Column(name = "CUSTOMER_ID", nullable = false, length = 50)
    private String customerId;

    @Column(name = "RECIPIENT_EMAIL", nullable = false, length = 255)
    private String recipientEmail;

    @Column(name = "SUBJECT", nullable = false, length = 255)
    private String subject;

    @Lob
    @Column(name = "MESSAGE_BODY", nullable = false)
    private String messageBody;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 20)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "SENT_AT")
    private LocalDateTime sentAt;

    @Column(name = "FAILURE_REASON", length = 1000)
    private String failureReason;

    @OneToMany(
        mappedBy = "notification",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    @Builder.Default
    private List<NotificationDelivery> deliveryAttempts = new ArrayList<>();
}