package com.netbanking.auth.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.auth-topic:auth-events}")
    private String topic;

    public void publish(String type, String customerId, String actorId, Map<String, Object> data) {
        try {
            var event = Map.of(
                    "eventId", UUID.randomUUID().toString(),
                    "eventType", type,
                    "customerId", customerId == null ? "" : customerId,
                    "actorId", actorId == null ? "" : actorId,
                    "timestamp", Instant.now().toString(),
                    "data", data == null ? Map.of() : data
            );
            kafkaTemplate.send(topic, customerId == null ? UUID.randomUUID().toString() : customerId, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Kafka async delivery failed for event [{}]: {}", type, ex.getMessage());
                        } else {
                            log.debug("Kafka event [{}] delivered successfully to topic {}", type, topic);
                        }
                    });
        } catch (Exception ex) {
            log.warn("Unable to publish Kafka event [{}] (Kafka may be unavailable): {}", type, ex.getMessage());
        }
    }
}
