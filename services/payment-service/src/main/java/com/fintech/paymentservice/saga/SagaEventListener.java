package com.fintech.paymentservice.saga;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.paymentservice.entity.Payment;
import com.fintech.paymentservice.model.PaymentStatus;
import com.fintech.paymentservice.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
public class SagaEventListener {

    private static final Logger logger = LoggerFactory.getLogger(SagaEventListener.class);

    private final SagaRepository sagaRepository;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;

    public SagaEventListener(SagaRepository sagaRepository,
                             PaymentRepository paymentRepository,
                             ObjectMapper objectMapper) {
        this.sagaRepository = sagaRepository;
        this.paymentRepository = paymentRepository;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
        topics = "${kafka.topics.saga-transaction-completed:saga.transaction.completed}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleTransactionCompleted(String message, Acknowledgment acknowledgment) {
        try {
            logger.info("Received transaction completed event: {}", message);

            JsonNode json = objectMapper.readTree(message);
            String paymentId = json.get("paymentId").asText();

            Optional<SagaState> sagaOpt = sagaRepository.findByPaymentId(paymentId);
            if (sagaOpt.isPresent()) {
                SagaState saga = sagaOpt.get();
                saga.setStatus(SagaStatus.COMPLETED);
                saga.setCompletedAt(Instant.now());
                sagaRepository.save(saga);

                logger.info("Saga completed for paymentId: {}", paymentId);
            }

            // Update payment status
            Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setCompletedAt(Instant.now());
                paymentRepository.save(payment);
            }

            acknowledgment.acknowledge();
        } catch (Exception e) {
            logger.error("Error handling transaction completed event: {}", e.getMessage(), e);
            acknowledgment.acknowledge(); // Acknowledge to prevent infinite retry; DLQ handles failures
        }
    }

    @KafkaListener(
        topics = "${kafka.topics.saga-transaction-failed:saga.transaction.failed}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleTransactionFailed(String message, Acknowledgment acknowledgment) {
        try {
            logger.info("Received transaction failed event: {}", message);

            JsonNode json = objectMapper.readTree(message);
            String paymentId = json.get("paymentId").asText();
            String reason = json.has("reason") ? json.get("reason").asText() : "Unknown failure";

            Optional<SagaState> sagaOpt = sagaRepository.findByPaymentId(paymentId);
            if (sagaOpt.isPresent()) {
                SagaState saga = sagaOpt.get();
                saga.setStatus(SagaStatus.FAILED);
                saga.setFailureReason(reason);
                saga.setCompletedAt(Instant.now());
                sagaRepository.save(saga);

                logger.info("Saga marked as failed for paymentId: {}. Reason: {}", paymentId, reason);
            }

            // Update payment status
            Optional<Payment> paymentOpt = paymentRepository.findById(paymentId);
            if (paymentOpt.isPresent()) {
                Payment payment = paymentOpt.get();
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason(reason);
                payment.setFailedAt(Instant.now());
                paymentRepository.save(payment);
            }

            acknowledgment.acknowledge();
        } catch (Exception e) {
            logger.error("Error handling transaction failed event: {}", e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }
}
