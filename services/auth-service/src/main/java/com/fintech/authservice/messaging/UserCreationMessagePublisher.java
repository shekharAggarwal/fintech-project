package com.fintech.authservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class UserCreationMessagePublisher {

    private static final Logger logger = LoggerFactory.getLogger(UserCreationMessagePublisher.class);

    final private RabbitTemplate rabbitTemplate;

    final private ObjectMapper objectMapper;

    @Value("${rabbitmq.exchange.user}")
    private String userExchange;

    @Value("${rabbitmq.routing-key.user-creation}")
    private String userCreationRoutingKey;

    public UserCreationMessagePublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish user creation message to RabbitMQ
     */
    public void publishUserCreationMessage(Object userCreationMessage) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(userCreationMessage);

            rabbitTemplate.convertAndSend(userExchange, userCreationRoutingKey, jsonMessage);

            logger.info("Published user creation message to exchange: {} with routing key: {}",
                    userExchange, userCreationRoutingKey);

        } catch (Exception e) {
            logger.error("Failed to publish user creation message", e);
            throw new RuntimeException("Failed to publish user creation message", e);
        }
    }
}
