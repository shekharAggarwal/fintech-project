package com.fintech.ledgerservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.ledgerservice.dto.message.TransactionCompletedMessage;
import com.fintech.ledgerservice.service.LedgerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Objects;


@Component
public class TransactionCompletedEventListener {

    private static final Logger logger = LoggerFactory.getLogger(TransactionCompletedEventListener.class);

    private final ObjectMapper objectMapper;
    private final LedgerService ledgerService;

    public TransactionCompletedEventListener(ObjectMapper objectMapper, LedgerService ledgerService) {
        this.objectMapper = objectMapper;
        this.ledgerService = ledgerService;
    }


    /**
     * Listen for user creation messages from Kafka
     * Receives JSON strings from auth-service and deserializes to UserCreationMessage
     */
    @KafkaListener(topics = "${kafka.topics.transaction-completed}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleAccountCreationMessage(
            @Payload String jsonMessage,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            logger.info("Received user creation message from topic: {}, partition: {}, offset: {}",
                    topic, partition, offset);

            // Deserialize JSON string to AccountCreationMessage object
            TransactionCompletedMessage transactionCompletedMessage = objectMapper.readValue(jsonMessage, TransactionCompletedMessage.class);

            logger.info("Successfully parsed account creation message for userId: {}", transactionCompletedMessage.getUserId());

            if (Objects.equals(transactionCompletedMessage.getStatus(), "COMPLETED")) {
                // Process the user creation
                ledgerService.createLedgerEntry(transactionCompletedMessage);
            }

            logger.info("Successfully processed user account creation for userId: {}", transactionCompletedMessage.getUserId());

            // Manually acknowledge the message
            acknowledgment.acknowledge();

        } catch (Exception e) {
            logger.error("Failed to process user account creation message from topic: {}, partition: {}, offset: {}. Message: {}",
                    topic, partition, offset, jsonMessage, e);

            // Don't acknowledge on error - this will cause the message to be retried
            // In production, you might want to implement a retry mechanism with dead letter topic
            throw new RuntimeException("Failed to process user creation message", e);
        }
    }
}