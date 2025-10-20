package com.fintech.notificationservice.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.notificationservice.dto.EmailMessageDto;
import com.fintech.notificationservice.service.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NotificationListener {
    
    private final NotificationService notificationService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = "notification.email")
    public void handleEmailNotification(Map<String, Object> message) {
        try {
            String to = (String) message.get("to");
            String subject = (String) message.get("subject");
            String body = (String) message.get("body");
            String type = (String) message.get("type");
            
            EmailMessageDto emailMessage = new EmailMessageDto(to, subject, body, type);
            notificationService.sendEmail(emailMessage);
            
        } catch (Exception e) {
            System.err.println("Error processing email notification: " + e.getMessage());
        }
    }
}
