package com.fintech.notificationservice.service;

import com.fintech.notificationservice.dto.EmailMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Retryable(
            retryFor = {MailException.class, RuntimeException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 4000)
    )
    public void sendEmail(EmailMessage emailMessage) {
        logger.info("Attempting to send email to: {} (retryable)", emailMessage.getTo());

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(emailMessage.getTo());
        message.setSubject(emailMessage.getSubject());
        message.setText(emailMessage.getBody());
        message.setFrom("noreply@fintechbank.com");

        mailSender.send(message);

        logger.info("Email sent successfully to: {}", emailMessage.getTo());
    }

    @Recover
    public void recoverSendEmail(Exception e, EmailMessage emailMessage) {
        logger.error("All retry attempts exhausted for email to: {}. Error: {}",
                emailMessage.getTo(), e.getMessage(), e);
        throw new RuntimeException("Failed to send email after all retries to: " + emailMessage.getTo(), e);
    }
}
