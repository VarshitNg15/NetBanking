package com.netbanking.notification.kafka.event;

public record AuditEvent(
        String eventId,
        String eventType,
        String actorId,
        String actorRole,
        String customerId,
        String resourceType,
        String resourceId,
        String action,
        String description,
        String ipAddress,
        String userAgent
) {}
