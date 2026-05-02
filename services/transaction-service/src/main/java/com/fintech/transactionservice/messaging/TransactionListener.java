package com.fintech.transactionservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.transactionservice.dto.message.PaymentInitiatedEvent;
import com.fintech.transactionservice.entity.Transaction;
import com.fintech.transactionservice.service.IdempotencyService;
import com.fintech.transactionservice.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class TransactionListener {

    private static final Logger logger = LoggerFactory.getLogger(TransactionListener.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TransactionService transactionService;
    private final IdempotencyService idempotencyService;

    public TransactionListener(TransactionService transactionService, IdempotencyService idempotencyService) {
        this.transactionService = transactionService;
        this.idempotencyService = idempotencyService;
    }

    @KafkaListener(topics = "${kafka.topics.transaction-initiate}", groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onPaymentAuthorized(@Payload String message,
                                    @Header(KafkaHeaders.RECEIVED_KEY) String key,
                                    Acknowledgment acknowledgment) {

        logger.info("Processing transaction event with key: {}", key);

        try {
            // Parse the JSON message into PaymentAuthorizedEvent DTO
            PaymentInitiatedEvent paymentEvent = objectMapper.readValue(message, PaymentInitiatedEvent.class);

            logger.info("Parsed payment authorized event: paymentId={}, userId={}, amount={}",
                    paymentEvent.getPaymentId(), paymentEvent.getUserId(), paymentEvent.getAmount());

            // Idempotency check using paymentId as key
            Optional<Transaction> existing = idempotencyService.checkDuplicate(paymentEvent.getPaymentId());
            if (existing.isPresent()) {
                logger.warn("Duplicate event detected for paymentId: {}, skipping processing", paymentEvent.getPaymentId());
                acknowledgment.acknowledge();
                return;
            }

            // Create transaction record
            Transaction transaction = transactionService.createTransaction(paymentEvent);

            // Store idempotency mapping
            idempotencyService.storeInRedis(paymentEvent.getPaymentId(), transaction.getTxnId());

            logger.info("Successfully processed payment authorized event for paymentId: {}, transactionId: {}",
                    paymentEvent.getPaymentId(), transaction.getTxnId());

            acknowledgment.acknowledge();

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            // Non-retryable: malformed message, acknowledge to avoid poison pill
            logger.error("Failed to deserialize message for key: {}. Acknowledging to prevent infinite retry. Error: {}",
                    key, e.getMessage(), e);
            acknowledgment.acknowledge();

        } catch (IllegalStateException e) {
            // Duplicate transaction - already processed, acknowledge
            logger.warn("Duplicate transaction for key: {} - {}", key, e.getMessage());
            acknowledgment.acknowledge();

        } catch (Exception e) {
            // Retryable errors: don't acknowledge, let DLQ/retry handle it
            logger.error("Failed to process payment authorized event for key: {}. " +
                            "Message will be retried or sent to DLQ. Error: {}",
                    key, e.getMessage(), e);
            throw new RuntimeException("Transaction processing failed for key: " + key, e);
        }
    }
}
