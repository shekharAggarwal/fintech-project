package com.fintech.userservice.messaging;

import com.fintech.userservice.dto.UserCreationMessage;
import com.fintech.userservice.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserCreationMessageListener {
    
    private static final Logger logger = LoggerFactory.getLogger(UserCreationMessageListener.class);
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Listen for user creation messages from RabbitMQ
     */
    @RabbitListener(queues = "${rabbitmq.queue.user-creation}")
    public void handleUserCreationMessage(String message) {
        try {
            logger.info("Received user creation message: {}", message);
            
            // Parse JSON message to UserCreationMessage object
            UserCreationMessage userCreationMessage = objectMapper.readValue(message, UserCreationMessage.class);
            
            // Process the user creation
            userService.createUserProfile(userCreationMessage);
            
            logger.info("Successfully processed user creation for userId: {}", userCreationMessage.getUserId());
            
        } catch (Exception e) {
            logger.error("Failed to process user creation message: {}", message, e);
            // In production, you might want to send this to a dead letter queue
            throw new RuntimeException("Failed to process user creation message", e);
        }
    }
}
