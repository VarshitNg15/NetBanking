package com.netbanking.auth.event;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.auth-topic:auth-events}")
    private String topic;

    public void publish(String type, String customerId, String actorId, Map<String, Object> data) {
        var event = Map.of(
                "eventId", UUID.randomUUID().toString(),
                "eventType", type,
                "customerId", customerId == null ? "" : customerId,
                "actorId", actorId == null ? "" : actorId,
                "timestamp", Instant.now().toString(),
                "data", data == null ? Map.of() : data
        );
        kafkaTemplate.send(topic, customerId == null ? UUID.randomUUID().toString() : customerId, event);
    }
}
