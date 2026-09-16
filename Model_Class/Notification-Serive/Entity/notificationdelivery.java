package com.netbanking.notificationservice.model;

import com.netbanking.notificationservice.model.enums.DeliveryStatus;
import com.netbanking.notificationservice.model.enums.SmtpProvider;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "NOTIFICATION_DELIVERY")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "DELIVERY_ID")
    private Long deliveryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "NOTIFICATION_ID",
        nullable = false,
        foreignKey = @ForeignKey(name = "FK_NOTIFICATION_DELIVERY_NOTIFICATION")
    )
    private Notification notification;

    @Column(name = "ATTEMPT_NUMBER", nullable = false)
    private Integer attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "SMTP_PROVIDER", nullable = false, length = 50)
    @Builder.Default
    private SmtpProvider smtpProvider = SmtpProvider.GOOGLE_SMTP;

    @Enumerated(EnumType.STRING)
    @Column(name = "DELIVERY_STATUS", nullable = false, length = 20)
    private DeliveryStatus deliveryStatus;

    @Column(name = "ATTEMPTED_AT", nullable = false)
    private LocalDateTime attemptedAt;

    @Column(name = "DELIVERED_AT")
    private LocalDateTime deliveredAt;

    @Column(name = "ERROR_CODE", length = 100)
    private String errorCode;

    @Column(name = "ERROR_MESSAGE", length = 1000)
    private String errorMessage;

    @Column(name = "RESPONSE_MESSAGE", length = 1000)
    private String responseMessage;

    @Column(name = "MESSAGE_ID", length = 255)
    private String messageId;
}