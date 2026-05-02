package com.fintech.reportingservice.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.reportingservice.entity.ReportEvent;
import com.fintech.reportingservice.repository.ReportEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka event listener that consumes transaction and payment events
 * and stores them in the report_events table for report generation.
 */
@Component
public class TransactionEventListener {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventListener.class);

    private final ReportEventRepository reportEventRepository;
    private final ObjectMapper objectMapper;

    public TransactionEventListener(ReportEventRepository reportEventRepository, ObjectMapper objectMapper) {
        this.reportEventRepository = reportEventRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * Listens for transaction-completed events.
     */
    @KafkaListener(
            topics = "${kafka.topics.transaction-completed:transaction-completed}",
            groupId = "${spring.kafka.consumer.group-id:reporting-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleTransactionCompleted(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        processEvent("transaction-completed", "transaction-service", payload, topic, acknowledgment);
    }

    /**
     * Listens for transaction-failed events.
     */
    @KafkaListener(
            topics = "${kafka.topics.transaction-failed:transaction-failed}",
            groupId = "${spring.kafka.consumer.group-id:reporting-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleTransactionFailed(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        processEvent("transaction-failed", "transaction-service", payload, topic, acknowledgment);
    }

    /**
     * Listens for payment events.
     */
    @KafkaListener(
            topics = "${kafka.topics.payment-events:payment-events}",
            groupId = "${spring.kafka.consumer.group-id:reporting-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handlePaymentEvents(
            @Payload String payload,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            Acknowledgment acknowledgment) {

        processEvent("payment-event", "payment-service", payload, topic, acknowledgment);
    }

    /**
     * Common event processing logic: parse, extract metadata, persist, acknowledge.
     */
    private void processEvent(String eventType, String sourceService, String payload, String topic, Acknowledgment acknowledgment) {
        try {
            log.debug("Received {} event from topic {}", eventType, topic);

            ReportEvent event = new ReportEvent();
            event.setEventType(eventType);
            event.setPayload(payload);
            event.setSourceService(sourceService);

            // Attempt to extract accountId and correlationId from JSON payload
            try {
                JsonNode jsonNode = objectMapper.readTree(payload);
                if (jsonNode.has("accountId")) {
                    event.setAccountId(jsonNode.get("accountId").asText());
                } else if (jsonNode.has("account_id")) {
                    event.setAccountId(jsonNode.get("account_id").asText());
                }
                if (jsonNode.has("correlationId")) {
                    event.setCorrelationId(jsonNode.get("correlationId").asText());
                } else if (jsonNode.has("transactionId")) {
                    event.setCorrelationId(jsonNode.get("transactionId").asText());
                }
            } catch (JsonProcessingException e) {
                log.warn("Could not parse payload JSON for metadata extraction: {}", e.getMessage());
            }

            reportEventRepository.save(event);
            acknowledgment.acknowledge();

            log.info("Stored {} event successfully. AccountId: {}", eventType, event.getAccountId());
        } catch (Exception e) {
            log.error("Failed to process {} event from topic {}: {}", eventType, topic, e.getMessage(), e);
            // Do not acknowledge — message will be redelivered
        }
    }
}
