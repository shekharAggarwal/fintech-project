package com.fintech.transactionservice.listener;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Listener for Dead Letter Topic messages.
 * Processes messages that have exhausted all retries from the main consumer.
 * Logs detailed information for manual investigation and alerting.
 */
@Component
public class DeadLetterTopicListener {

    private static final Logger logger = LoggerFactory.getLogger(DeadLetterTopicListener.class);

    @KafkaListener(
            topics = "${kafka.topics.transaction-initiate}.DLT",
            groupId = "${spring.kafka.consumer.group-id}-dlt",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onDeadLetterMessage(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        logger.error("=== DEAD LETTER RECEIVED ===");
        logger.error("DLT Message - Topic: {}, Partition: {}, Offset: {}, Key: {}",
                record.topic(), record.partition(), record.offset(), record.key());
        logger.error("DLT Message - Value: {}", record.value());
        logger.error("DLT Message - Timestamp: {}, Headers: {}", record.timestamp(), record.headers());

        // Extract original exception info from headers if available
        record.headers().forEach(header -> {
            if (header.key().contains("exception")) {
                logger.error("DLT Header - {}: {}", header.key(), new String(header.value()));
            }
        });

        // TODO: Implement alerting (e.g., PagerDuty, Slack notification)
        // TODO: Implement dead letter storage for later replay

        // Acknowledge to prevent re-processing the DLT message infinitely
        acknowledgment.acknowledge();
        logger.error("=== DEAD LETTER PROCESSED (acknowledged) ===");
    }
}
