package com.fintech.userservice.dto.message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.userservice.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class UserCreationKafkaListener {

    private static final Logger logger = LoggerFactory.getLogger(UserCreationKafkaListener.class);

    private final UserService userService;
    private final ObjectMapper objectMapper;

    public UserCreationKafkaListener(UserService userService, ObjectMapper objectMapper) {
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    /**
     * Listen for user creation messages from Kafka
     * Receives JSON strings from auth-service and deserializes to UserCreationMessage
     */
    @KafkaListener(topics = "${kafka.topics.user-creation}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleUserCreationMessage(
            @Payload String jsonMessage,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            logger.info("Received user creation message from topic: {}, partition: {}, offset: {}",
                    topic, partition, offset);

            // Deserialize JSON string to UserCreationMessage object
            UserCreationMessage userCreationMessage = objectMapper.readValue(jsonMessage, UserCreationMessage.class);

            logger.info("Successfully parsed user creation message for userId: {}", userCreationMessage.getUserId());

            // Process the user creation
            userService.createUserProfile(userCreationMessage);

            logger.info("Successfully processed user creation for userId: {}", userCreationMessage.getUserId());

            // Manually acknowledge the message
            acknowledgment.acknowledge();

        } catch (Exception e) {
            logger.error("Failed to process user creation message from topic: {}, partition: {}, offset: {}. Message: {}",
                    topic, partition, offset, jsonMessage, e);

            // Don't acknowledge on error - this will cause the message to be retried
            // In production, you might want to implement a retry mechanism with dead letter topic
            throw new RuntimeException("Failed to process user creation message", e);
        }
    }
}
