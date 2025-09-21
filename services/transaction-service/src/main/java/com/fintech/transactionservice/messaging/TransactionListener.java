package com.fintech.transactionservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.transactionservice.dto.message.PaymentInitiatedEvent;
import com.fintech.transactionservice.entity.Transaction;
import com.fintech.transactionservice.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class TransactionListener {

    private static final Logger logger = LoggerFactory.getLogger(TransactionListener.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TransactionService transactionService;

    public TransactionListener(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @KafkaListener(topics = "${kafka.topics.transaction-initiate}", groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentAuthorized(@Payload String message,
                                    @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                    Acknowledgment acknowledgment) {

        logger.info("Processing transaction event with key: {}", key);

        try {
            // Parse the JSON message into PaymentAuthorizedEvent DTO
            PaymentInitiatedEvent paymentEvent = objectMapper.readValue(message, PaymentInitiatedEvent.class);

            logger.info("Parsed payment authorized event: {}", paymentEvent);

            // Create transaction record
            Transaction transaction = transactionService.createTransaction(paymentEvent);

            logger.info("Successfully processed payment authorized event for paymentId: {}, transactionId: {}",
                    paymentEvent.getPaymentId(), transaction.getTxnId());

            acknowledgment.acknowledge();

        } catch (Exception e) {
            logger.error("Failed to process payment authorized event for key: {} - {}",
                    key, e.getMessage(), e);

            // For serious errors, we might want to retry - don't acknowledge
            // For now, acknowledge to avoid infinite retries
            acknowledgment.acknowledge();
        }
    }
}
