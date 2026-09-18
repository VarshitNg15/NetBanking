package com.netbanking.transaction.kafka.producer;

import com.netbanking.transaction.entity.Transaction;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.transaction-events:transaction-events}")
    private String transactionTopic;

    public void publishTransactionEvent(Transaction transaction) {
        publishTransactionEvent(transaction, null);
    }

    public void publishTransactionEvent(Transaction transaction, String recipientEmail) {
        TransactionEvent event = new TransactionEvent(
                transaction.getTransactionReference(),
                transaction.getTransactionType(),
                transaction.getTransactionStatus(),
                transaction.getCustomerId(),
                recipientEmail,
                transaction.getSourceAccountId(),
                transaction.getDestinationAccountId(),
                transaction.getAmount(),
                transaction.getCurrency()
        );
        kafkaTemplate.send(transactionTopic, transaction.getTransactionReference(), event);
    }

    public record TransactionEvent(
            String transactionReference,
            String transactionType,
            String transactionStatus,
            String customerId,
            String recipientEmail,
            Long sourceAccountId,
            Long destinationAccountId,
            java.math.BigDecimal amount,
            String currency
    ) {}
}
