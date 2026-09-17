package com.netbanking.notification.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netbanking.notification.dto.NotificationCreateRequest;
import com.netbanking.notification.entity.Notification;
import com.netbanking.notification.kafka.event.NotificationEvent;
import com.netbanking.notification.service.EmailService;
import com.netbanking.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @KafkaListener(topics = "notification-events", groupId = "notification-service-group")
    public void consume(String payload) {
        try {
            NotificationEvent event = objectMapper.readValue(payload, NotificationEvent.class);
            Notification notification = notificationService.create(
                    new NotificationCreateRequest(
                            event.eventId(),
                            event.eventType(),
                            event.customerId(),
                            event.recipientEmail(),
                            event.subject(),
                            event.messageBody()
                    )
            );
            emailService.send(notification);
        } catch (Exception ex) {
            log.error("Failed to process notification event", ex);
        }
    }
}
