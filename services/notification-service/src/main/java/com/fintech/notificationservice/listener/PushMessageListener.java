package com.fintech.notificationservice.listener;

import com.fintech.notificationservice.config.RabbitMQConfig;
import com.fintech.notificationservice.dto.PushMessageDto;
import com.fintech.notificationservice.service.PushNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PushMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(PushMessageListener.class);

    private final PushNotificationService pushNotificationService;

    public PushMessageListener(PushNotificationService pushNotificationService) {
        this.pushNotificationService = pushNotificationService;
    }

    @RabbitListener(queues = RabbitMQConfig.PUSH_QUEUE)
    public void handlePushMessage(PushMessageDto pushMessage) {
        try {
            logger.info("Received push message for userId: {}", pushMessage.getUserId());
            pushNotificationService.sendPushNotification(pushMessage);
            logger.info("Push notification processed successfully for userId: {}", pushMessage.getUserId());
        } catch (Exception e) {
            logger.error("Failed to process push notification for userId: {} - Error: {}",
                    pushMessage.getUserId(), e.getMessage(), e);
            throw e; // Re-throw to let RabbitMQ send to DLQ
        }
    }
}
