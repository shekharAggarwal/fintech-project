package com.fintech.userservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
public class KycEventKafkaPublisher {

    private static final Logger logger = LoggerFactory.getLogger(KycEventKafkaPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.kyc-events:kyc-events}")
    private String kycEventsTopic;

    public KycEventKafkaPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishKycSubmitted(String userId, Long documentId, String documentType) {
        publishEvent("KYC_SUBMITTED", userId, Map.of(
                "userId", userId,
                "documentId", documentId,
                "documentType", documentType,
                "timestamp", Instant.now().toString()
        ));
    }

    public void publishKycApproved(String userId, Long documentId, String verifiedBy) {
        publishEvent("KYC_APPROVED", userId, Map.of(
                "userId", userId,
                "documentId", documentId,
                "verifiedBy", verifiedBy,
                "timestamp", Instant.now().toString()
        ));
    }

    public void publishKycRejected(String userId, Long documentId, String rejectionReason, String rejectedBy) {
        publishEvent("KYC_REJECTED", userId, Map.of(
                "userId", userId,
                "documentId", documentId,
                "rejectionReason", rejectionReason,
                "rejectedBy", rejectedBy,
                "timestamp", Instant.now().toString()
        ));
    }

    private void publishEvent(String eventType, String key, Map<String, Object> payload) {
        try {
            Map<String, Object> event = Map.of(
                    "eventType", eventType,
                    "payload", payload
            );
            String jsonMessage = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(kycEventsTopic, key, jsonMessage)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            logger.info("Published {} event to topic: {} with offset: {} for userId: {}",
                                    eventType, kycEventsTopic, result.getRecordMetadata().offset(), key);
                        } else {
                            logger.error("Failed to publish {} event to topic: {} for userId: {}",
                                    eventType, kycEventsTopic, key, ex);
                        }
                    });
        } catch (Exception e) {
            logger.error("Failed to serialize {} event for userId: {}", eventType, key, e);
        }
    }
}
