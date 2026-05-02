package com.fintech.notificationservice.service;

import com.fintech.notificationservice.dto.SmsMessageDto;
import com.fintech.notificationservice.entity.Notification;
import com.fintech.notificationservice.provider.sms.SmsProvider;
import com.fintech.notificationservice.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SmsService {

    private static final Logger logger = LoggerFactory.getLogger(SmsService.class);

    private final SmsProvider smsProvider;
    private final NotificationRepository notificationRepository;

    public SmsService(SmsProvider smsProvider, NotificationRepository notificationRepository) {
        this.smsProvider = smsProvider;
        this.notificationRepository = notificationRepository;
    }

    @Retryable(
            retryFor = {RuntimeException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 4000)
    )
    public void sendSms(SmsMessageDto smsMessage) {
        logger.info("Sending SMS to: {} via provider: {} (retryable)",
                smsMessage.getPhoneNumber(), smsProvider.getProviderName());

        Notification notification = new Notification(
                smsMessage.getPhoneNumber(),
                "SMS: " + smsMessage.getType(),
                smsMessage.getMessage(),
                Notification.NotificationType.SMS
        );
        notification.setAttemptCount(1);
        notification.setLastAttemptAt(LocalDateTime.now());

        boolean success = smsProvider.sendSms(smsMessage.getPhoneNumber(), smsMessage.getMessage());

        if (success) {
            notification.setStatus(Notification.NotificationStatus.SENT);
            notification.setSentAt(LocalDateTime.now());
            logger.info("SMS sent successfully to: {}", smsMessage.getPhoneNumber());
        } else {
            notification.setStatus(Notification.NotificationStatus.FAILED);
            notification.setErrorMessage("SMS provider returned failure");
            throw new RuntimeException("SMS provider returned failure for: " + smsMessage.getPhoneNumber());
        }

        notificationRepository.save(notification);
    }

    @Recover
    public void recoverSendSms(Exception e, SmsMessageDto smsMessage) {
        logger.error("All retry attempts exhausted for SMS to: {}. Error: {}",
                smsMessage.getPhoneNumber(), e.getMessage(), e);

        Notification notification = new Notification(
                smsMessage.getPhoneNumber(),
                "SMS: " + smsMessage.getType(),
                smsMessage.getMessage(),
                Notification.NotificationType.SMS
        );
        notification.setStatus(Notification.NotificationStatus.FAILED);
        notification.setErrorMessage("All retries exhausted: " + e.getMessage());
        notification.setAttemptCount(3);
        notification.setLastAttemptAt(LocalDateTime.now());
        notificationRepository.save(notification);

        throw new RuntimeException("Failed to send SMS after all retries to: " + smsMessage.getPhoneNumber(), e);
    }
}
