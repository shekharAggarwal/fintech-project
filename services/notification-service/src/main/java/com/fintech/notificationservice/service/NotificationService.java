package com.fintech.notificationservice.service;

import com.fintech.notificationservice.dto.EmailMessageDto;
import com.fintech.notificationservice.entity.Notification;
import com.fintech.notificationservice.repository.NotificationRepository;
import io.micrometer.tracing.annotation.SpanTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    private final JavaMailSender emailSender;

    public NotificationService(NotificationRepository notificationRepository, JavaMailSender emailSender) {
        this.notificationRepository = notificationRepository;
        this.emailSender = emailSender;
    }

    public CompletableFuture<Void> sendEmail(@SpanTag("recipient") EmailMessageDto emailMessage) {
        logger.info("Sending email to: {} with subject: {}", emailMessage.getTo(), emailMessage.getSubject());

        return CompletableFuture.runAsync(() -> {
            // Create notification record
            Notification notification = new Notification(
                    emailMessage.getTo(),
                    emailMessage.getSubject(),
                    emailMessage.getBody(),
                    Notification.NotificationType.EMAIL
            );

            try {
                // Send email
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(emailMessage.getTo());
                message.setSubject(emailMessage.getSubject());
                message.setText(emailMessage.getBody());
                message.setFrom("noreply@fintechbank.com");

                emailSender.send(message);

                // Update notification status
                notification.setStatus(Notification.NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());

                logger.info("Email sent successfully to: {}", emailMessage.getTo());

            } catch (Exception e) {
                logger.error("Failed to send email to: {}", emailMessage.getTo(), e);
                // Update notification status on failure
                notification.setStatus(Notification.NotificationStatus.FAILED);
                notification.setErrorMessage(e.getMessage());
                throw new RuntimeException("Email sending failed", e);
            } finally {
                // Save notification record
                notificationRepository.save(notification);
            }
        });
    }

    // Fallback method for circuit breaker
    public CompletableFuture<Void> sendEmailFallback(EmailMessageDto emailMessage, Exception ex) {
        logger.error("Email service fallback triggered for recipient: {}", emailMessage.getTo(), ex);

        return CompletableFuture.runAsync(() -> {
            // Create notification record with failed status
            Notification notification = new Notification(
                    emailMessage.getTo(),
                    emailMessage.getSubject(),
                    emailMessage.getBody(),
                    Notification.NotificationType.EMAIL
            );
            notification.setStatus(Notification.NotificationStatus.FAILED);
            notification.setErrorMessage("Service temporarily unavailable: " + ex.getMessage());

            // Save notification record
            notificationRepository.save(notification);
        });
    }

    // ...existing code...

    public void sendWelcomeEmail(String recipientEmail, String fullName, String accountNumber, Double balance) {
        EmailMessageDto emailMessage = new EmailMessageDto();
        emailMessage.setTo(recipientEmail);
        emailMessage.setSubject("Welcome to FinTech Bank - Account Created Successfully");
        emailMessage.setBody(String.format(
                "Dear %s,\n\n" +
                        "Welcome to FinTech Bank! Your account has been created successfully.\n\n" +
                        "Account Details:\n" +
                        "Account Number: %s\n" +
                        "Account Balance: $%.2f\n" +
                        "Email: %s\n\n" +
                        "You can now login to your account using your email and password.\n\n" +
                        "Thank you for choosing FinTech Bank!\n\n" +
                        "Best regards,\n" +
                        "FinTech Bank Team",
                fullName, accountNumber, balance, recipientEmail
        ));
        emailMessage.setType("WELCOME");

        sendEmail(emailMessage);
    }
}
