package com.fintech.transactionservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Publishes dispute and reversal lifecycle events to Kafka.
 */
@Component
public class DisputeEventPublisher {

    private static final Logger logger = LoggerFactory.getLogger(DisputeEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.dispute-events}")
    private String disputeEventsTopic;

    @Value("${kafka.topics.reversal-events}")
    private String reversalEventsTopic;

    public DisputeEventPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishDisputeOpened(String disputeId, String transactionId, String disputeType) {
        publishEvent(disputeEventsTopic, disputeId, Map.of(
                "eventType", "DISPUTE_OPENED",
                "disputeId", disputeId,
                "transactionId", transactionId,
                "disputeType", disputeType
        ));
    }

    public void publishDisputeResolved(String disputeId, String transactionId, String resolution) {
        publishEvent(disputeEventsTopic, disputeId, Map.of(
                "eventType", "DISPUTE_RESOLVED",
                "disputeId", disputeId,
                "transactionId", transactionId,
                "resolution", resolution
        ));
    }

    public void publishDisputeCancelled(String disputeId, String transactionId) {
        publishEvent(disputeEventsTopic, disputeId, Map.of(
                "eventType", "DISPUTE_CANCELLED",
                "disputeId", disputeId,
                "transactionId", transactionId
        ));
    }

    public void publishReversalInitiated(String reversalId, String originalTransactionId, String reversalType) {
        publishEvent(reversalEventsTopic, reversalId, Map.of(
                "eventType", "REVERSAL_INITIATED",
                "reversalId", reversalId,
                "originalTransactionId", originalTransactionId,
                "reversalType", reversalType
        ));
    }

    public void publishReversalCompleted(String reversalId, String originalTransactionId, String reversalTransactionId) {
        publishEvent(reversalEventsTopic, reversalId, Map.of(
                "eventType", "REVERSAL_COMPLETED",
                "reversalId", reversalId,
                "originalTransactionId", originalTransactionId,
                "reversalTransactionId", reversalTransactionId
        ));
    }

    private void publishEvent(String topic, String key, Map<String, String> payload) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, key, jsonMessage)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            logger.info("Published event to topic: {} with key: {}", topic, key);
                        } else {
                            logger.error("Failed to publish event to topic: {} with key: {}", topic, key, ex);
                        }
                    });
        } catch (Exception e) {
            logger.error("Failed to serialize event for topic: {} with key: {}", topic, key, e);
        }
    }
}
