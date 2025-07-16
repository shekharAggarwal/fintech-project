package com.fintech.notificationservice.listener;

import com.fintech.notificationservice.dto.EmailMessage;
import com.fintech.notificationservice.service.EmailService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class EmailMessageListener {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailMessageListener.class);
    
    @Autowired
    private EmailService emailService;
    
    @RabbitListener(queues = "notification.email.queue")
    public void handleEmailMessage(EmailMessage emailMessage) {
        try {
            logger.info("Received email message for: {}", emailMessage.getTo());
            emailService.sendEmail(emailMessage);
            logger.info("Email processed successfully for: {}", emailMessage.getTo());
        } catch (Exception e) {
            logger.error("Failed to process email message for: {} - Error: {}", 
                        emailMessage.getTo(), e.getMessage());
            // In a production environment, you might want to send this to a dead letter queue
            // or implement retry logic
        }
    }
}
