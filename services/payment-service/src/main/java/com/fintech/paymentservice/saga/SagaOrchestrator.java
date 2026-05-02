package com.fintech.paymentservice.saga;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.paymentservice.entity.Payment;
import com.fintech.paymentservice.model.PaymentStatus;
import com.fintech.paymentservice.repository.PaymentRepository;
import com.fintech.paymentservice.service.BalanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class SagaOrchestrator {

    private static final Logger logger = LoggerFactory.getLogger(SagaOrchestrator.class);

    private final SagaRepository sagaRepository;
    private final PaymentRepository paymentRepository;
    private final BalanceService balanceService;
    private final ObjectMapper objectMapper;

    public SagaOrchestrator(SagaRepository sagaRepository,
                            PaymentRepository paymentRepository,
                            BalanceService balanceService,
                            ObjectMapper objectMapper) {
        this.sagaRepository = sagaRepository;
        this.paymentRepository = paymentRepository;
        this.balanceService = balanceService;
        this.objectMapper = objectMapper;
    }

    /**
     * Execute the payment saga for a given payment
     */
    @Transactional
    public SagaState executePaymentSaga(Payment payment) {
        logger.info("Starting payment saga for paymentId: {}", payment.getPaymentId());

        SagaState saga = new SagaState(payment.getPaymentId());
        saga = sagaRepository.save(saga);

        List<SagaStep> steps = List.of(
            SagaStep.VALIDATE_FUNDS,
            SagaStep.HOLD_FUNDS,
            SagaStep.INITIATE_TRANSACTION,
            SagaStep.PROCESS,
            SagaStep.DEBIT,
            SagaStep.CREDIT,
            SagaStep.COMPLETE
        );

        List<String> completedSteps = new ArrayList<>();
        Map<String, Object> compensationData = new HashMap<>();

        for (SagaStep step : steps) {
            try {
                saga.setCurrentStep(step);
                sagaRepository.save(saga);

                executeStep(step, payment, compensationData);

                completedSteps.add(step.name());
                saga.setCompletedSteps(toJson(completedSteps));
                saga.setCompensationData(toJson(compensationData));
                sagaRepository.save(saga);

                logger.debug("Saga step {} completed for paymentId: {}", step, payment.getPaymentId());

            } catch (Exception e) {
                logger.error("Saga step {} failed for paymentId: {}. Reason: {}", step, payment.getPaymentId(), e.getMessage());

                saga.setStatus(SagaStatus.COMPENSATING);
                saga.setFailureReason(e.getMessage());
                sagaRepository.save(saga);

                // Compensate in reverse order
                compensate(saga, payment, completedSteps, compensationData);
                return saga;
            }
        }

        // All steps completed successfully
        saga.setStatus(SagaStatus.COMPLETED);
        saga.setCompletedAt(Instant.now());
        sagaRepository.save(saga);

        logger.info("Payment saga completed successfully for paymentId: {}", payment.getPaymentId());
        return saga;
    }

    /**
     * Compensate (rollback) completed steps in reverse order
     */
    @Transactional
    public void compensate(SagaState saga, Payment payment, List<String> completedSteps, Map<String, Object> compensationData) {
        logger.info("Starting compensation for saga paymentId: {}", saga.getPaymentId());

        Collections.reverse(completedSteps);

        for (String stepName : completedSteps) {
            try {
                SagaStep step = SagaStep.valueOf(stepName);
                compensateStep(step, payment, compensationData);
                logger.debug("Compensated step {} for paymentId: {}", step, saga.getPaymentId());
            } catch (Exception e) {
                logger.error("Failed to compensate step {} for paymentId: {}. Manual intervention required.", stepName, saga.getPaymentId(), e);
                saga.setStatus(SagaStatus.FAILED);
                saga.setFailureReason("Compensation failed at step: " + stepName + ". " + e.getMessage());
                sagaRepository.save(saga);
                return;
            }
        }

        saga.setStatus(SagaStatus.COMPENSATED);
        saga.setCompletedAt(Instant.now());
        sagaRepository.save(saga);

        // Update payment status to FAILED
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason("Saga compensation completed: " + saga.getFailureReason());
        payment.setFailedAt(Instant.now());
        paymentRepository.save(payment);

        logger.info("Compensation completed for saga paymentId: {}", saga.getPaymentId());
    }

    private void executeStep(SagaStep step, Payment payment, Map<String, Object> compensationData) {
        switch (step) {
            case VALIDATE_FUNDS -> {
                boolean hasFunds = balanceService.hasSufficientFunds(payment.getFromAccount(), payment.getAmount());
                if (!hasFunds) {
                    throw new RuntimeException("Insufficient funds in account: " + payment.getFromAccount());
                }
            }
            case HOLD_FUNDS -> {
                balanceService.placeHold(payment.getFromAccount(), payment.getAmount(), "Hold for payment: " + payment.getPaymentId());
                compensationData.put("holdAccount", payment.getFromAccount());
                compensationData.put("holdAmount", payment.getAmount().toString());
            }
            case INITIATE_TRANSACTION -> {
                payment.setStatus(PaymentStatus.PROCESSING);
                payment.setProcessingStartedAt(Instant.now());
                paymentRepository.save(payment);
            }
            case PROCESS -> {
                // External processing step - in real system would call payment processor
                logger.debug("Processing payment: {}", payment.getPaymentId());
            }
            case DEBIT -> {
                balanceService.releaseHold(payment.getFromAccount(), payment.getAmount(), "Release hold for debit: " + payment.getPaymentId());
                balanceService.debit(payment.getFromAccount(), payment.getAmount(), "Payment debit: " + payment.getPaymentId());
                compensationData.put("debited", "true");
                compensationData.put("debitAccount", payment.getFromAccount());
                compensationData.put("debitAmount", payment.getAmount().toString());
            }
            case CREDIT -> {
                balanceService.credit(payment.getToAccount(), payment.getAmount(), "Payment credit: " + payment.getPaymentId());
                compensationData.put("credited", "true");
                compensationData.put("creditAccount", payment.getToAccount());
                compensationData.put("creditAmount", payment.getAmount().toString());
            }
            case COMPLETE -> {
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setCompletedAt(Instant.now());
                paymentRepository.save(payment);
            }
        }
    }

    private void compensateStep(SagaStep step, Payment payment, Map<String, Object> compensationData) {
        switch (step) {
            case HOLD_FUNDS -> {
                String holdAccount = (String) compensationData.get("holdAccount");
                String holdAmountStr = (String) compensationData.get("holdAmount");
                if (holdAccount != null && holdAmountStr != null) {
                    balanceService.releaseHold(holdAccount, new BigDecimal(holdAmountStr), "Compensation: release hold for " + payment.getPaymentId());
                }
            }
            case DEBIT -> {
                if ("true".equals(compensationData.get("debited"))) {
                    String debitAccount = (String) compensationData.get("debitAccount");
                    String debitAmount = (String) compensationData.get("debitAmount");
                    balanceService.credit(debitAccount, new BigDecimal(debitAmount), "Compensation: reverse debit for " + payment.getPaymentId());
                }
            }
            case CREDIT -> {
                if ("true".equals(compensationData.get("credited"))) {
                    String creditAccount = (String) compensationData.get("creditAccount");
                    String creditAmount = (String) compensationData.get("creditAmount");
                    balanceService.debit(creditAccount, new BigDecimal(creditAmount), "Compensation: reverse credit for " + payment.getPaymentId());
                }
            }
            case INITIATE_TRANSACTION, PROCESS, COMPLETE -> {
                // Status revert handled at the end of compensation
            }
            case VALIDATE_FUNDS -> {
                // No compensation needed for validation
            }
        }
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize to JSON", e);
            return "{}";
        }
    }
}
