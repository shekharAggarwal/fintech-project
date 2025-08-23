package com.fintech.authorizationservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.authorizationservice.dto.request.UserRoleRegistrationMessage;
import com.fintech.authorizationservice.service.AuthzService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class UserRoleKafkaListener {

    private static final Logger logger = LoggerFactory.getLogger(UserRoleKafkaListener.class);

    private final AuthzService authzService;
    private final ObjectMapper objectMapper;


    public UserRoleKafkaListener(AuthzService authzService, ObjectMapper objectMapper) {
        this.authzService = authzService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${kafka.topics.user-role-registration}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleUserRoleRegistrationMessage(
            @Payload String jsonMessage,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            UserRoleRegistrationMessage userRoleMessage = objectMapper.readValue(jsonMessage, UserRoleRegistrationMessage.class);
            logger.info("Received user role registration message from topic: {}, partition: {}, offset: {} for userId: {} with role: {}",
                    topic, partition, offset, userRoleMessage.getUserId(), userRoleMessage.getRole());

            // Register user role using the authorization service
            authzService.registerUserRole(userRoleMessage.getUserId(), userRoleMessage.getRole());

            logger.info("User role registered successfully: userId={}, role={}",
                    userRoleMessage.getUserId(), userRoleMessage.getRole());

            // Manually acknowledge the message
            acknowledgment.acknowledge();

        } catch (Exception e) {
            logger.error("Failed to process user role registration message for userId: {} with role: {}, topic: {}, partition: {}, offset: {}",
                    jsonMessage, jsonMessage, topic, partition, offset, e);

            // Don't acknowledge on error - this will cause the message to be retried
            throw new RuntimeException("Failed to process user role registration message", e);
        }
    }
}
