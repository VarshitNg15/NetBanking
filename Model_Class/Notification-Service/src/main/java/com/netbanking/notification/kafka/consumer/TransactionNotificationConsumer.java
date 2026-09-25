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
public class TransactionNotificationConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @KafkaListener(topics = "${notification.kafka.transaction-topic:transaction-events}", groupId = "${notification.kafka.group-id:notification-service-group}")
    public void consume(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            String ref = root.path("transactionReference").asText();
            String status = root.path("transactionStatus").asText();
            String amount = root.path("amount").asText();
            String currency = root.path("currency").asText("INR");
            String customerId = root.path("customerId").asText("UNKNOWN");
            String recipientEmail = root.path("recipientEmail").asText(null);

            if (recipientEmail != null && !recipientEmail.isBlank() && !recipientEmail.equalsIgnoreCase("null")) {
                String txnType = root.path("transactionType").asText("");
                boolean isCredit = "TRANSFER_CREDIT".equalsIgnoreCase(txnType);

                String subject = isCredit
                        ? "NetBanking Alert: Account Credited (" + currency + " " + amount + ")"
                        : "NetBanking Alert: Transaction " + status + " (" + currency + " " + amount + ")";

                String body = isCredit
                        ? "Dear Customer,\n\nYour account has been credited with " + currency + " " + amount + " via transaction " + ref + ".\n\nWarm regards,\nNetBanking Alerts"
                        : "Dear Customer,\n\nYour transfer of " + currency + " " + amount + " (Ref: " + ref + ") has been processed with status: " + status + ".\n\nIf you did not initiate this transfer, please contact customer support immediately.\n\nWarm regards,\nNetBanking Alerts";

                log.info("Dispatching {} transaction alert email to {} for txn {}", isCredit ? "CREDIT" : "DEBIT", recipientEmail, ref);
                Notification notification = notificationService.create(new NotificationCreateRequest(
                        "TXN-" + UUID.randomUUID(),
                        isCredit ? "TRANSFER_CREDIT" : "TRANSACTION_ALERT",
                        customerId,
                        recipientEmail,
                        subject,
                        body
                ));
                emailService.send(notification);
            }
        } catch (Exception ex) {
            log.error("Failed to process transaction event for email notification: {}", ex.getMessage(), ex);
        }
    }
}
