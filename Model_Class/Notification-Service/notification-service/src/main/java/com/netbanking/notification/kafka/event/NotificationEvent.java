package com.netbanking.notification.kafka.event;

public record NotificationEvent(
        String eventId,
        String eventType,
        String customerId,
        String recipientEmail,
        String subject,
        String messageBody
) {}
