package com.fintech.paymentservice.saga;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.paymentservice.entity.Payment;
import com.fintech.paymentservice.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class SagaTimeoutScheduler {

    private static final Logger logger = LoggerFactory.getLogger(SagaTimeoutScheduler.class);

    private final SagaRepository sagaRepository;
    private final SagaOrchestrator sagaOrchestrator;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;

    @Value("${saga.timeout.minutes:5}")
    private int timeoutMinutes;

    public SagaTimeoutScheduler(SagaRepository sagaRepository,
                                 SagaOrchestrator sagaOrchestrator,
                                 PaymentRepository paymentRepository,
                                 ObjectMapper objectMapper) {
        this.sagaRepository = sagaRepository;
        this.sagaOrchestrator = sagaOrchestrator;
        this.paymentRepository = paymentRepository;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${saga.timeout.check-interval-ms:60000}")
    @Transactional
    public void detectStuckSagas() {
        Instant timeout = Instant.now().minus(timeoutMinutes, ChronoUnit.MINUTES);
        List<SagaState> stuckSagas = sagaRepository.findStuckSagas(SagaStatus.IN_PROGRESS, timeout);

        if (!stuckSagas.isEmpty()) {
            logger.warn("Found {} stuck sagas (timeout: {} minutes)", stuckSagas.size(), timeoutMinutes);
        }

        for (SagaState saga : stuckSagas) {
            try {
                logger.info("Handling stuck saga for paymentId: {}. Current step: {}", saga.getPaymentId(), saga.getCurrentStep());

                saga.setStatus(SagaStatus.COMPENSATING);
                saga.setFailureReason("Saga timed out after " + timeoutMinutes + " minutes at step: " + saga.getCurrentStep());
                sagaRepository.save(saga);

                // Get the payment and compensate
                Optional<Payment> paymentOpt = paymentRepository.findById(saga.getPaymentId());
                if (paymentOpt.isPresent()) {
                    Payment payment = paymentOpt.get();
                    List<String> completedSteps = parseCompletedSteps(saga.getCompletedSteps());
                    Map<String, Object> compensationData = parseCompensationData(saga.getCompensationData());

                    sagaOrchestrator.compensate(saga, payment, completedSteps, compensationData);
                } else {
                    saga.setStatus(SagaStatus.FAILED);
                    saga.setFailureReason("Payment not found for stuck saga");
                    sagaRepository.save(saga);
                }

            } catch (Exception e) {
                logger.error("Failed to handle stuck saga for paymentId: {}", saga.getPaymentId(), e);
                saga.setStatus(SagaStatus.FAILED);
                saga.setFailureReason("Timeout handling failed: " + e.getMessage());
                sagaRepository.save(saga);
            }
        }
    }

    private List<String> parseCompletedSteps(String json) {
        try {
            if (json == null || json.isBlank()) return new ArrayList<>();
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            logger.error("Failed to parse completed steps JSON", e);
            return new ArrayList<>();
        }
    }

    private Map<String, Object> parseCompensationData(String json) {
        try {
            if (json == null || json.isBlank()) return new HashMap<>();
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            logger.error("Failed to parse compensation data JSON", e);
            return new HashMap<>();
        }
    }
}
