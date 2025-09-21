package com.fintech.transactionservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.transactionservice.dto.message.TransactionCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class TransactionCompletedEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(TransactionCompletedEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.topics.transaction-completed}")
    private String transactionCompletedTopic;

    final ObjectMapper objectMapper;

    public TransactionCompletedEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish user role registration message to Kafka for authorization service
     */
    public void publishTransactionCompleted(TransactionCompletedEvent transactionCompletedEvent) {
        try {
            // Convert object to JSON string
            String jsonMessage = objectMapper.writeValueAsString(transactionCompletedEvent);

            kafkaTemplate.send(transactionCompletedTopic, transactionCompletedEvent.getTxnId(), jsonMessage)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {


                            logger.info("Published ledger entry message to topic: {} with offset: {} for tnxId: {} with userId: {} and amount: {} and description: {}",
                                    transactionCompletedTopic,
                                    result.getRecordMetadata().offset(),
                                    transactionCompletedEvent.getTxnId(),
                                    transactionCompletedEvent.getUserId(),
                                    transactionCompletedEvent.getAmount(),
                                    transactionCompletedEvent.getDescription()
                            );

                        } else {
                            logger.error("Failed to publish ledger entry message to topic: {} with offset: {} for tnxId: {} with userId: {} and amount: {} and description: {}",
                                    transactionCompletedTopic,
                                    result.getRecordMetadata().offset(),
                                    transactionCompletedEvent.getTxnId(),
                                    transactionCompletedEvent.getUserId(),
                                    transactionCompletedEvent.getAmount(),
                                    transactionCompletedEvent.getDescription(), ex);
                            throw new RuntimeException("Failed to publish ledger entry message", ex);
                        }
                    });

        } catch (Exception e) {
            logger.error("Failed to publish ledger entry message to topic: {} for tnxId: {} with userId: {} and amount: {} and description: {}",
                    transactionCompletedTopic,
                    transactionCompletedEvent.getTxnId(),
                    transactionCompletedEvent.getUserId(),
                    transactionCompletedEvent.getAmount(),
                    transactionCompletedEvent.getDescription(), e);
            throw new RuntimeException("Failed to publish ledger entry message", e);
        }
    }
}