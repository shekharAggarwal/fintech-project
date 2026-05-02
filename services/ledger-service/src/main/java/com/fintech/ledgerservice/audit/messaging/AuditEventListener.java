package com.fintech.ledgerservice.audit.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.ledgerservice.audit.entity.*;
import com.fintech.ledgerservice.audit.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka listener that captures events from multiple topics and records them
 * in the immutable audit trail.
 */
@Component
public class AuditEventListener {

    private static final Logger logger = LoggerFactory.getLogger(AuditEventListener.class);

    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public AuditEventListener(ObjectMapper objectMapper, AuditService auditService) {
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    @KafkaListener(topics = "${kafka.topics.payment-events}", groupId = "${spring.kafka.consumer.group-id}-audit")
    public void handlePaymentEvent(
            @Payload String jsonMessage,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        processAuditEvent(jsonMessage, topic, partition, offset, acknowledgment,
                AuditEventType.PAYMENT_RECEIVED, ResourceType.PAYMENT);
    }

    @KafkaListener(topics = "${kafka.topics.transaction-completed}", groupId = "${spring.kafka.consumer.group-id}-audit")
    public void handleTransactionCompletedEvent(
            @Payload String jsonMessage,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        processAuditEvent(jsonMessage, topic, partition, offset, acknowledgment,
                AuditEventType.TRANSACTION_COMPLETED, ResourceType.TRANSACTION);
    }

    @KafkaListener(topics = "${kafka.topics.transaction-failed}", groupId = "${spring.kafka.consumer.group-id}-audit")
    public void handleTransactionFailedEvent(
            @Payload String jsonMessage,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        processAuditEvent(jsonMessage, topic, partition, offset, acknowledgment,
                AuditEventType.TRANSACTION_FAILED, ResourceType.TRANSACTION);
    }

    @KafkaListener(topics = "${kafka.topics.user-events}", groupId = "${spring.kafka.consumer.group-id}-audit")
    public void handleUserEvent(
            @Payload String jsonMessage,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        processAuditEvent(jsonMessage, topic, partition, offset, acknowledgment,
                AuditEventType.USER_CREATED, ResourceType.USER);
    }

    @KafkaListener(topics = "${kafka.topics.session-events}", groupId = "${spring.kafka.consumer.group-id}-audit")
    public void handleSessionEvent(
            @Payload String jsonMessage,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        processAuditEvent(jsonMessage, topic, partition, offset, acknowledgment,
                AuditEventType.SESSION_STARTED, ResourceType.SESSION);
    }

    private void processAuditEvent(String jsonMessage, String topic, int partition, long offset,
                                   Acknowledgment acknowledgment, AuditEventType defaultEventType,
                                   ResourceType defaultResourceType) {
        try {
            logger.debug("Audit listener received from topic: {}, partition: {}, offset: {}", topic, partition, offset);

            JsonNode node = objectMapper.readTree(jsonMessage);

            String actorId = extractField(node, "userId", "actorId", "SYSTEM");
            String resourceId = extractField(node, "txnId", "paymentId", "resourceId", "UNKNOWN");
            String status = extractField(node, "status", null);

            // Determine event type based on status if available
            AuditEventType eventType = defaultEventType;
            if ("FAILED".equals(status)) {
                eventType = AuditEventType.TRANSACTION_FAILED;
            } else if ("COMPLETED".equals(status)) {
                eventType = AuditEventType.TRANSACTION_COMPLETED;
            }

            auditService.recordEvent(
                    eventType,
                    actorId,
                    ActorType.SYSTEM,
                    defaultResourceType,
                    resourceId,
                    AuditAction.CREATE,
                    jsonMessage,
                    null
            );

            acknowledgment.acknowledge();

        } catch (Exception e) {
            logger.error("Failed to process audit event from topic: {}, partition: {}, offset: {}",
                    topic, partition, offset, e);
            throw new RuntimeException("Failed to process audit event", e);
        }
    }

    private String extractField(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            if (fieldName == null) continue;
            JsonNode fieldNode = node.get(fieldName);
            if (fieldNode != null && !fieldNode.isNull()) {
                return fieldNode.asText();
            }
        }
        return fieldNames[fieldNames.length - 1]; // Return last as default
    }
}
