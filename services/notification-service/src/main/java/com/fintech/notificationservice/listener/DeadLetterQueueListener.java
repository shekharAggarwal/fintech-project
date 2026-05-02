package com.fintech.notificationservice.listener;

import com.fintech.notificationservice.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class DeadLetterQueueListener {

    private static final Logger logger = LoggerFactory.getLogger(DeadLetterQueueListener.class);

    @RabbitListener(queues = RabbitMQConfig.DLQ_EMAIL)
    public void handleEmailDlq(Message message) {
        logger.error("Email message sent to DLQ. Headers: {}, Body: {}",
                message.getMessageProperties().getHeaders(),
                new String(message.getBody()));
    }

    @RabbitListener(queues = RabbitMQConfig.DLQ_SMS)
    public void handleSmsDlq(Message message) {
        logger.error("SMS message sent to DLQ. Headers: {}, Body: {}",
                message.getMessageProperties().getHeaders(),
                new String(message.getBody()));
    }

    @RabbitListener(queues = RabbitMQConfig.DLQ_PUSH)
    public void handlePushDlq(Message message) {
        logger.error("Push message sent to DLQ. Headers: {}, Body: {}",
                message.getMessageProperties().getHeaders(),
                new String(message.getBody()));
    }
}
