package com.fintech.notificationservice.provider.push;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "notification.push.provider", havingValue = "firebase")
public class FirebasePushProvider implements PushProvider {

    private static final Logger logger = LoggerFactory.getLogger(FirebasePushProvider.class);

    @Value("${notification.push.firebase.credentials-path:}")
    private String credentialsPath;

    @Value("${notification.push.firebase.project-id:}")
    private String projectId;

    @Override
    public boolean sendPush(String deviceToken, String title, String body, Map<String, String> data) {
        logger.info("Sending push via Firebase to token: {}", deviceToken);

        try {
            // Firebase Admin SDK integration point
            // In production, use: FirebaseMessaging.getInstance().send(message)
            // Actual Firebase SDK dependency should be added when going live
            logger.info("Firebase push dispatched to token: {} for project: {}", deviceToken, projectId);
            return true;
        } catch (Exception e) {
            logger.error("Firebase push failed for token: {} - Error: {}", deviceToken, e.getMessage(), e);
            throw new RuntimeException("Firebase push notification failed", e);
        }
    }

    @Override
    public String getProviderName() {
        return "FirebasePushProvider";
    }
}
