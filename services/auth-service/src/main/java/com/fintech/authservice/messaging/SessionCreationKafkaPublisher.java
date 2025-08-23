package com.fintech.authservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.authservice.dto.response.SessionCreationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class SessionCreationKafkaPublisher {

    private static final Logger logger = LoggerFactory.getLogger(SessionCreationKafkaPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.session-creation}")
    private String sessionCreationTopic;

    public SessionCreationKafkaPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish session creation message to Kafka for authorization service
     */
    public void publishSessionCreationMessage(SessionCreationMessage sessionCreationMessage) {
        try {
            // Convert object to JSON string
            String jsonMessage = objectMapper.writeValueAsString(sessionCreationMessage);

            kafkaTemplate.send(sessionCreationTopic, sessionCreationMessage.getSessionId(), jsonMessage)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        logger.info("Published session creation message to topic: {} with offset: {} for sessionId: {}",
                            sessionCreationTopic, result.getRecordMetadata().offset(), sessionCreationMessage.getSessionId());
                    } else {
                        logger.error("Failed to publish session creation message to topic: {} for sessionId: {}", 
                            sessionCreationTopic, sessionCreationMessage.getSessionId(), ex);
                        throw new RuntimeException("Failed to publish session creation message", ex);
                    }
                });

        } catch (Exception e) {
            logger.error("Failed to publish session creation message to topic: {} for sessionId: {}", 
                sessionCreationTopic, sessionCreationMessage.getSessionId(), e);
            throw new RuntimeException("Failed to publish session creation message", e);
        }
    }
}
