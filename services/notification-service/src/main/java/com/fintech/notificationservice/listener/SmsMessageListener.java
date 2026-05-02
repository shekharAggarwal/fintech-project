package com.fintech.notificationservice.listener;

import com.fintech.notificationservice.config.RabbitMQConfig;
import com.fintech.notificationservice.dto.SmsMessageDto;
import com.fintech.notificationservice.service.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class SmsMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(SmsMessageListener.class);

    private final SmsService smsService;

    public SmsMessageListener(SmsService smsService) {
        this.smsService = smsService;
    }

    @RabbitListener(queues = RabbitMQConfig.SMS_QUEUE)
    public void handleSmsMessage(SmsMessageDto smsMessage) {
        try {
            logger.info("Received SMS message for: {}", smsMessage.getPhoneNumber());
            smsService.sendSms(smsMessage);
            logger.info("SMS processed successfully for: {}", smsMessage.getPhoneNumber());
        } catch (Exception e) {
            logger.error("Failed to process SMS for: {} - Error: {}",
                    smsMessage.getPhoneNumber(), e.getMessage(), e);
            throw e; // Re-throw to let RabbitMQ send to DLQ
        }
    }
}
