package com.fintech.notificationservice.service;

import com.fintech.notificationservice.dto.PushMessageDto;
import com.fintech.notificationservice.entity.DeviceToken;
import com.fintech.notificationservice.entity.Notification;
import com.fintech.notificationservice.provider.push.PushProvider;
import com.fintech.notificationservice.repository.DeviceTokenRepository;
import com.fintech.notificationservice.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PushNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(PushNotificationService.class);

    private final PushProvider pushProvider;
    private final DeviceTokenRepository deviceTokenRepository;
    private final NotificationRepository notificationRepository;

    public PushNotificationService(PushProvider pushProvider,
                                   DeviceTokenRepository deviceTokenRepository,
                                   NotificationRepository notificationRepository) {
        this.pushProvider = pushProvider;
        this.deviceTokenRepository = deviceTokenRepository;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Fan-out push notification to all enabled device tokens for a user.
     */
    public void sendPushNotification(PushMessageDto pushMessage) {
        String userId = pushMessage.getUserId();
        logger.info("Resolving device tokens for userId: {}", userId);

        List<DeviceToken> tokens = deviceTokenRepository.findByUserIdAndEnabledTrue(userId);

        if (tokens.isEmpty()) {
            logger.warn("No enabled device tokens found for userId: {}", userId);
            return;
        }

        logger.info("Found {} device token(s) for userId: {}. Sending push via {}",
                tokens.size(), userId, pushProvider.getProviderName());

        Map<String, String> data = new HashMap<>();
        data.put("type", pushMessage.getType());

        for (DeviceToken token : tokens) {
            try {
                boolean success = pushProvider.sendPush(
                        token.getDeviceToken(),
                        pushMessage.getTitle(),
                        pushMessage.getBody(),
                        data
                );

                Notification notification = new Notification(
                        userId,
                        pushMessage.getTitle(),
                        pushMessage.getBody(),
                        Notification.NotificationType.PUSH
                );
                notification.setAttemptCount(1);
                notification.setLastAttemptAt(LocalDateTime.now());

                if (success) {
                    notification.setStatus(Notification.NotificationStatus.SENT);
                    notification.setSentAt(LocalDateTime.now());
                    logger.info("Push sent to token: {} (platform: {})", token.getDeviceToken(), token.getPlatform());
                } else {
                    notification.setStatus(Notification.NotificationStatus.FAILED);
                    notification.setErrorMessage("Push provider returned failure");
                    logger.warn("Push failed for token: {}", token.getDeviceToken());
                }

                notificationRepository.save(notification);

            } catch (Exception e) {
                logger.error("Push notification failed for token: {} - Error: {}",
                        token.getDeviceToken(), e.getMessage(), e);
            }
        }
    }
}
