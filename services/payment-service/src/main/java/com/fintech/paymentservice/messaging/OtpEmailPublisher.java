package com.fintech.paymentservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.paymentservice.dto.message.OtpNotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OtpEmailPublisher {

    private static final Logger logger = LoggerFactory.getLogger(OtpEmailPublisher.class);


    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${rabbitmq.exchange.notification}")
    private String notificationExchange;

    @Value("${rabbitmq.routing-key.email}")
    private String emailRoutingKey;

    public OtpEmailPublisher(RabbitTemplate rabbitTemplate, ObjectMapper objectMapper) {
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish transaction otp email notification to RabbitMQ for notification service
     */
    public void publishOtpEmail(OtpNotificationEvent emailMessage) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(emailMessage);

            rabbitTemplate.convertAndSend(notificationExchange, emailRoutingKey, jsonMessage);

            logger.info("Published transaction otp email notification to exchange: {} with routing key: {}",
                    notificationExchange, emailRoutingKey);

        } catch (Exception e) {

            logger.error("Failed to publish transaction otp email notification", e);
            throw new RuntimeException("Failed to publish transaction otp email notification", e);
        }
    }

}