package com.fintech.notificationservice.service;

import com.fintech.notificationservice.dto.EmailMessageDto;
import com.fintech.notificationservice.dto.PushMessageDto;
import com.fintech.notificationservice.dto.SmsMessageDto;
import com.fintech.notificationservice.entity.Notification;
import com.fintech.notificationservice.entity.NotificationPreference;
import com.fintech.notificationservice.repository.NotificationRepository;
import io.micrometer.tracing.annotation.SpanTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final JavaMailSender emailSender;
    private final RabbitTemplate rabbitTemplate;
    private final PreferenceService preferenceService;

    public NotificationService(NotificationRepository notificationRepository,
                               JavaMailSender emailSender,
                               RabbitTemplate rabbitTemplate,
                               PreferenceService preferenceService) {
        this.notificationRepository = notificationRepository;
        this.emailSender = emailSender;
        this.rabbitTemplate = rabbitTemplate;
        this.preferenceService = preferenceService;
    }

    // ─── Multi-Channel Routing ──────────────────────────────────────────────────

    /**
     * Routes notifications to appropriate channels based on type:
     * - TRANSACTION_ALERT → all channels (email + sms + push)
     * - OTP → SMS preferred (fallback to email)
     * - MARKETING → respect user preferences
     * - WELCOME / default → email only
     */
    public void routeNotification(String userId, String recipient, String subject,
                                  String body, String type) {
        logger.info("Routing notification type={} for userId={}", type, userId);

        switch (type.toUpperCase()) {
            case "TRANSACTION_ALERT" -> sendToAllChannels(userId, recipient, subject, body);
            case "OTP" -> sendOtp(userId, recipient, subject, body);
            case "MARKETING" -> sendWithPreferences(userId, recipient, subject, body);
            default -> sendEmailOnly(recipient, subject, body, type);
        }
    }

    private void sendToAllChannels(String userId, String recipient, String subject, String body) {
        logger.info("Sending transaction alert to all channels for userId={}", userId);

        // Email
        sendEmailOnly(recipient, subject, body, "TRANSACTION_ALERT");

        // SMS
        SmsMessageDto sms = new SmsMessageDto(recipient, body, "TRANSACTION_ALERT", userId);
        rabbitTemplate.convertAndSend("notification.exchange", "notification.sms", sms);

        // Push
        PushMessageDto push = new PushMessageDto(userId, subject, body, "TRANSACTION_ALERT");
        rabbitTemplate.convertAndSend("notification.exchange", "notification.push", push);
    }

    private void sendOtp(String userId, String recipient, String subject, String body) {
        logger.info("Sending OTP notification, SMS preferred for userId={}", userId);

        // OTP always goes via SMS as primary
        SmsMessageDto sms = new SmsMessageDto(recipient, body, "OTP", userId);
        rabbitTemplate.convertAndSend("notification.exchange", "notification.sms", sms);

        // Also send email as fallback
        sendEmailOnly(recipient, subject, body, "OTP");
    }

    private void sendWithPreferences(String userId, String recipient, String subject, String body) {
        logger.info("Sending marketing notification with preference check for userId={}", userId);

        List<NotificationPreference> prefs = preferenceService.getPreferencesForUser(userId);

        for (NotificationPreference pref : prefs) {
            if (!pref.isEnabled()) continue;
            if (preferenceService.isInQuietHours(pref)) {
                logger.info("Skipping channel={} for userId={} due to quiet hours", pref.getChannel(), userId);
                continue;
            }

            switch (pref.getChannel()) {
                case EMAIL -> sendEmailOnly(recipient, subject, body, "MARKETING");
                case SMS -> {
                    SmsMessageDto sms = new SmsMessageDto(recipient, body, "MARKETING", userId);
                    rabbitTemplate.convertAndSend("notification.exchange", "notification.sms", sms);
                }
                case PUSH -> {
                    PushMessageDto push = new PushMessageDto(userId, subject, body, "MARKETING");
                    rabbitTemplate.convertAndSend("notification.exchange", "notification.push", push);
                }
            }
        }
    }

    private void sendEmailOnly(String recipient, String subject, String body, String type) {
        EmailMessageDto emailMessage = new EmailMessageDto(recipient, subject, body, type);
        sendEmail(emailMessage);
    }

    // ─── Email Sending ──────────────────────────────────────────────────────────

    public CompletableFuture<Void> sendEmail(@SpanTag("recipient") EmailMessageDto emailMessage) {
        logger.info("Sending email to: {} with subject: {}", emailMessage.getTo(), emailMessage.getSubject());

        return CompletableFuture.runAsync(() -> {
            Notification notification = new Notification(
                    emailMessage.getTo(),
                    emailMessage.getSubject(),
                    emailMessage.getBody(),
                    Notification.NotificationType.EMAIL
            );
            notification.setAttemptCount(1);
            notification.setLastAttemptAt(LocalDateTime.now());

            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(emailMessage.getTo());
                message.setSubject(emailMessage.getSubject());
                message.setText(emailMessage.getBody());
                message.setFrom("noreply@fintechbank.com");

                emailSender.send(message);

                notification.setStatus(Notification.NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());

                logger.info("Email sent successfully to: {}", emailMessage.getTo());

            } catch (Exception e) {
                logger.error("Failed to send email to: {}", emailMessage.getTo(), e);
                notification.setStatus(Notification.NotificationStatus.FAILED);
                notification.setErrorMessage(e.getMessage());
                throw new RuntimeException("Email sending failed", e);
            } finally {
                notificationRepository.save(notification);
            }
        });
    }

    // Fallback method for circuit breaker
    public CompletableFuture<Void> sendEmailFallback(EmailMessageDto emailMessage, Exception ex) {
        logger.error("Email service fallback triggered for recipient: {}", emailMessage.getTo(), ex);

        return CompletableFuture.runAsync(() -> {
            Notification notification = new Notification(
                    emailMessage.getTo(),
                    emailMessage.getSubject(),
                    emailMessage.getBody(),
                    Notification.NotificationType.EMAIL
            );
            notification.setStatus(Notification.NotificationStatus.FAILED);
            notification.setErrorMessage("Service temporarily unavailable: " + ex.getMessage());

            notificationRepository.save(notification);
        });
    }

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
