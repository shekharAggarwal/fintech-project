package com.fintech.notificationservice.listener;

import com.fintech.notificationservice.config.RabbitMQConfig;
import com.fintech.notificationservice.dto.EmailMessageDto;
import com.fintech.notificationservice.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NotificationListener {

    private static final Logger logger = LoggerFactory.getLogger(NotificationListener.class);

    private final NotificationService notificationService;

    public NotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    public void handleEmailNotification(Map<String, Object> message) {
        try {
            String to = (String) message.get("to");
            String subject = (String) message.get("subject");
            String body = (String) message.get("body");
            String type = (String) message.get("type");

            logger.info("Received email notification for recipient: {}", to);

            EmailMessageDto emailMessage = new EmailMessageDto(to, subject, body, type);
            notificationService.sendEmail(emailMessage);

            logger.info("Email notification processed successfully for: {}", to);
        } catch (Exception e) {
            logger.error("Error processing email notification: {}", e.getMessage(), e);
            throw e; // Re-throw to let RabbitMQ send to DLQ
        }
    }
}
