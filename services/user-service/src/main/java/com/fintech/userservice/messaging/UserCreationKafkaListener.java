package com.fintech.userservice.messaging;

import com.fintech.userservice.dto.UserCreationMessage;
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

    public UserCreationKafkaListener(UserService userService) {
        this.userService = userService;
    }

    /**
     * Listen for user creation messages from Kafka
     */
    @KafkaListener(topics = "${kafka.topics.user-creation}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleUserCreationMessage(
            @Payload UserCreationMessage userCreationMessage,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        
        try {
            logger.info("Received user creation message from topic: {}, partition: {}, offset: {} for userId: {}", 
                topic, partition, offset, userCreationMessage.getUserId());

            // Process the user creation
            userService.createUserProfile(userCreationMessage);

            logger.info("Successfully processed user creation for userId: {}", userCreationMessage.getUserId());
            
            // Manually acknowledge the message
            acknowledgment.acknowledge();

        } catch (Exception e) {
            logger.error("Failed to process user creation message for userId: {}, topic: {}, partition: {}, offset: {}", 
                userCreationMessage.getUserId(), topic, partition, offset, e);
            
            // Don't acknowledge on error - this will cause the message to be retried
            // In production, you might want to implement a retry mechanism with dead letter topic
            throw new RuntimeException("Failed to process user creation message", e);
        }
    }
}
