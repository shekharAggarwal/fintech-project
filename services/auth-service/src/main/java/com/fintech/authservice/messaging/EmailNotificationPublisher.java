package com.fintech.authservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationPublisher {

    private static final Logger logger = LoggerFactory.getLogger(EmailNotificationPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${rabbitmq.exchange.notification}")
    private String notificationExchange;

    @Value("${rabbitmq.routing-key.email}")
    private String emailRoutingKey;

    public EmailNotificationPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish email notification message to RabbitMQ for notification service
     */
    public void publishLoginFailureEmail(Object emailMessage) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(emailMessage);

            rabbitTemplate.convertAndSend(notificationExchange, emailRoutingKey, jsonMessage);

            logger.info("Published login failure email notification to exchange: {} with routing key: {}",
                    notificationExchange, emailRoutingKey);

        } catch (Exception e) {
            logger.error("Failed to publish login failure email notification", e);
            throw new RuntimeException("Failed to publish login failure email notification", e);
        }
    }

    /**
     * Publish general email notification message to RabbitMQ for notification service
     */
    public void publishEmailNotification(Object emailMessage) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(emailMessage);

            rabbitTemplate.convertAndSend(notificationExchange, emailRoutingKey, jsonMessage);

            logger.info("Published email notification to exchange: {} with routing key: {}",
                    notificationExchange, emailRoutingKey);

        } catch (Exception e) {
            logger.error("Failed to publish email notification", e);
            throw new RuntimeException("Failed to publish email notification", e);
        }
    }
}
