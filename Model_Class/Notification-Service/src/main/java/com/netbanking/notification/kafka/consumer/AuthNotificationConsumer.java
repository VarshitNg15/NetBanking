package com.netbanking.notification.kafka.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netbanking.notification.dto.NotificationCreateRequest;
import com.netbanking.notification.entity.Notification;
import com.netbanking.notification.service.EmailService;
import com.netbanking.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthNotificationConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @KafkaListener(topics = "${notification.kafka.auth-topic:auth-events}", groupId = "${notification.kafka.group-id:notification-service-group}")
    public void consume(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String eventType = root.path("eventType").asText();
            if ("OTP_ISSUED".equalsIgnoreCase(eventType)) {
                JsonNode data = root.path("data");
                String email = data.path("email").asText();
                String purpose = data.path("purpose").asText();
                String otp = data.path("otp").asText();
                String customerId = root.path("customerId").asText("UNKNOWN");
                String eventId = root.path("eventId").asText(UUID.randomUUID().toString());

                if (email != null && !email.isBlank() && otp != null && !otp.isBlank()) {
                    log.info("Dispatching OTP email to {} for purpose {}", email, purpose);
                    Notification notification = notificationService.create(new NotificationCreateRequest(
                            eventId,
                            "OTP_VERIFICATION",
                            customerId,
                            email,
                            "NetBanking Security Verification - OTP Code",
                            "Dear Customer,\n\nYour One-Time Password (OTP) for " + purpose + " is: " + otp + ".\n\nThis code is valid for 5 minutes. Please do not disclose this code to anyone.\n\nWarm regards,\nNetBanking Security Team"
                    ));
                    emailService.send(notification);
                }
            }
        } catch (Exception ex) {
            log.error("Failed to process auth event for email notification: {}", ex.getMessage(), ex);
        }
    }
}
