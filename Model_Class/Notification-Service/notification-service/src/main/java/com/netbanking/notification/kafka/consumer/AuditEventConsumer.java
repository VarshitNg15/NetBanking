package com.netbanking.notification.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netbanking.notification.entity.ActorRole;
import com.netbanking.notification.entity.AuditLog;
import com.netbanking.notification.kafka.event.AuditEvent;
import com.netbanking.notification.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventConsumer {

    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    @KafkaListener(topics = "audit-events", groupId = "notification-service-audit-group")
    public void consume(String payload) {
        try {
            AuditEvent event = objectMapper.readValue(payload, AuditEvent.class);
            auditService.record(AuditLog.builder()
                    .eventId(event.eventId())
                    .eventType(event.eventType())
                    .actorId(event.actorId())
                    .actorRole(event.actorRole() == null ? null : ActorRole.valueOf(event.actorRole()))
                    .customerId(event.customerId())
                    .resourceType(event.resourceType())
                    .resourceId(event.resourceId())
                    .action(event.action())
                    .description(event.description())
                    .ipAddress(event.ipAddress())
                    .userAgent(event.userAgent())
                    .build());
        } catch (Exception ex) {
            log.error("Failed to process audit event", ex);
        }
    }
}
