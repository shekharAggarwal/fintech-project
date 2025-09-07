package com.fintech.userservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.userservice.dto.message.UserRoleRegistrationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationKafkaPublisher {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationKafkaPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.topics.user-role-registration}")
    private String userRoleRegistrationTopic;

    final ObjectMapper objectMapper;

    public AuthorizationKafkaPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish user role registration message to Kafka for authorization service
     */
    public void publishUserRoleRegistration(UserRoleRegistrationMessage userRoleMessage) {
        try {
            // Convert object to JSON string
            String jsonMessage = objectMapper.writeValueAsString(userRoleMessage);

            kafkaTemplate.send(userRoleRegistrationTopic, userRoleMessage.getUserId(), jsonMessage)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            logger.info("Published user role registration message to topic: {} with offset: {} for userId: {} with role: {}",
                                    userRoleRegistrationTopic, result.getRecordMetadata().offset(),
                                    userRoleMessage.getUserId(), userRoleMessage.getRole());
                        } else {
                            logger.error("Failed to publish user role registration message to topic: {} for userId: {} with role: {}",
                                    userRoleRegistrationTopic, userRoleMessage.getUserId(), userRoleMessage.getRole(), ex);
                            throw new RuntimeException("Failed to publish user role registration message", ex);
                        }
                    });

        } catch (Exception e) {
            logger.error("Failed to publish user role registration message to topic: {} for userId: {} with role: {}",
                    userRoleRegistrationTopic, userRoleMessage.getUserId(), userRoleMessage.getRole(), e);
            throw new RuntimeException("Failed to publish user role registration message", e);
        }
    }
}
