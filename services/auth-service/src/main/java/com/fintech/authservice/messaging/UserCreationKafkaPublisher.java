package com.fintech.authservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class UserCreationKafkaPublisher {

    private static final Logger logger = LoggerFactory.getLogger(UserCreationKafkaPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.user-creation}")
    private String userCreationTopic;

    public UserCreationKafkaPublisher(KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish user creation message to Kafka for user service
     */
    public void publishUserCreationMessage(Object userCreationMessage) {
        try {
            kafkaTemplate.send(userCreationTopic, userCreationMessage)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        logger.info("Published user creation message to topic: {} with offset: {}",
                            userCreationTopic, result.getRecordMetadata().offset());
                    } else {
                        logger.error("Failed to publish user creation message to topic: {}", userCreationTopic, ex);
                        throw new RuntimeException("Failed to publish user creation message", ex);
                    }
                });

        } catch (Exception e) {
            logger.error("Failed to publish user creation message to topic: {}", userCreationTopic, e);
            throw new RuntimeException("Failed to publish user creation message", e);
        }
    }
}
