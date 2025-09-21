package com.fintech.paymentservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.paymentservice.dto.message.PaymentInitiatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TransactionPublisher {

    private static final Logger logger = LoggerFactory.getLogger(TransactionPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.topics.transaction-initiate}")
    private String transactionInitiateTopic;

    final ObjectMapper objectMapper;

    public TransactionPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish user role registration message to Kafka for authorization service
     */
    public void publishTransactionInitiate(PaymentInitiatedEvent paymentInitiatedEvent) {
        try {
            // Convert object to JSON string
            String jsonMessage = objectMapper.writeValueAsString(paymentInitiatedEvent);

            kafkaTemplate.send(transactionInitiateTopic, paymentInitiatedEvent.getPaymentId(), jsonMessage)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            logger.info("Published transaction initiate message to topic: {} with offset: {} for paymentId: {} with userId: {} and amount: {} and description: {} where account number from: {} and to: {}",
                                    transactionInitiateTopic, result.getRecordMetadata().offset(),
                                    paymentInitiatedEvent.getPaymentId(),
                                    paymentInitiatedEvent.getUserId(),
                                    paymentInitiatedEvent.getAmount(),
                                    paymentInitiatedEvent.getDescription(),
                                    paymentInitiatedEvent.getFromAccount(),
                                    paymentInitiatedEvent.getToAccount());

                        } else {
                            logger.error("Failed transaction initiate message to topic: {} with offset: {} for paymentId: {} with userId: {} and amount: {} and description: {} where account number from: {} and to: {}",
                                    transactionInitiateTopic, result.getRecordMetadata().offset(),
                                    paymentInitiatedEvent.getPaymentId(),
                                    paymentInitiatedEvent.getUserId(),
                                    paymentInitiatedEvent.getAmount(),
                                    paymentInitiatedEvent.getDescription(),
                                    paymentInitiatedEvent.getFromAccount(),
                                    paymentInitiatedEvent.getToAccount(), ex);
                            throw new RuntimeException("Failed to publish transaction initiate message", ex);
                        }
                    });

        } catch (Exception e) {
            logger.error("Failed transaction initiate message to topic: {}  for paymentId: {} with userId: {} and amount: {} and description: {} where account number from: {} and to: {}",
                    transactionInitiateTopic,
                    paymentInitiatedEvent.getPaymentId(),
                    paymentInitiatedEvent.getUserId(),
                    paymentInitiatedEvent.getAmount(),
                    paymentInitiatedEvent.getDescription(),
                    paymentInitiatedEvent.getFromAccount(),
                    paymentInitiatedEvent.getToAccount(), e);
            throw new RuntimeException("Failed to publish transaction initiate message", e);
        }
    }
}