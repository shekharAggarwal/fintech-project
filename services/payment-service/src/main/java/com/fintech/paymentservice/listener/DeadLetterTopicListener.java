package com.fintech.paymentservice.listener;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class DeadLetterTopicListener {

    private static final Logger logger = LoggerFactory.getLogger(DeadLetterTopicListener.class);

    /**
     * Listen to dead letter topics for monitoring and alerting.
     * Messages that reach the DLT have exhausted all retry attempts.
     */
    @KafkaListener(
        topicPattern = ".*\\.DLT",
        groupId = "${spring.kafka.consumer.group-id}-dlt",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleDeadLetter(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        logger.error("Dead letter received - Topic: {}, Key: {}, Partition: {}, Offset: {}",
            record.topic(), record.key(), record.partition(), record.offset());

        logger.error("Dead letter payload: {}", record.value());

        // Log headers for debugging (contains original exception info)
        record.headers().forEach(header -> {
            if (header.key().startsWith("kafka_dlt")) {
                logger.error("DLT Header - {}: {}", header.key(), new String(header.value()));
            }
        });

        // TODO: In production, integrate with alerting system (PagerDuty, Slack, etc.)
        // TODO: Optionally store in a dead letter database table for manual reprocessing

        acknowledgment.acknowledge();
        logger.info("Dead letter acknowledged - Topic: {}, Key: {}", record.topic(), record.key());
    }
}
