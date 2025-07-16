package com.fintech.notificationservice.service;

import com.fintech.notificationservice.dto.EmailMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class EmailService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    @Autowired
    private JavaMailSender mailSender;
    
    public void sendEmail(EmailMessage emailMessage) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(emailMessage.getTo());
            message.setSubject(emailMessage.getSubject());
            message.setText(emailMessage.getBody());
            message.setFrom("noreply@fintechbank.com");
            
            mailSender.send(message);
            
            logger.info("Email sent successfully to: {}", emailMessage.getTo());
        } catch (Exception e) {
            logger.error("Failed to send email to: {} - Error: {}", 
                        emailMessage.getTo(), e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
