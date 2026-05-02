package com.fintech.paymentservice.fraud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.paymentservice.entity.Payment;
import com.fintech.paymentservice.fraud.config.FraudDetectionProperties;
import com.fintech.paymentservice.fraud.entity.FraudAlert;
import com.fintech.paymentservice.fraud.exception.PaymentBlockedByFraudException;
import com.fintech.paymentservice.fraud.model.*;
import com.fintech.paymentservice.fraud.repository.FraudAlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FraudDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(FraudDetectionService.class);

    private final FraudRuleEngine ruleEngine;
    private final FraudAlertRepository alertRepository;
    private final FraudDetectionProperties properties;
    private final ObjectMapper objectMapper;

    public FraudDetectionService(FraudRuleEngine ruleEngine,
                                  FraudAlertRepository alertRepository,
                                  FraudDetectionProperties properties,
                                  ObjectMapper objectMapper) {
        this.ruleEngine = ruleEngine;
        this.alertRepository = alertRepository;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /**
     * Screen a transaction for fraud
     */
    @Transactional
    public FraudScreeningResult screenTransaction(Payment payment) {
        if (!properties.isEnabled()) {
            logger.debug("Fraud detection disabled, auto-approving payment: {}", payment.getPaymentId());
            return FraudScreeningResult.approve(payment.getPaymentId(), 0, List.of());
        }

        logger.info("Screening payment {} for fraud: amount={}, from={}, to={}",
            payment.getPaymentId(), payment.getAmount(), payment.getFromAccount(), payment.getToAccount());

        // Evaluate all rules
        List<RuleResult> ruleResults = ruleEngine.evaluateAll(payment);
        int totalScore = ruleEngine.calculateTotalScore(ruleResults);

        logger.info("Fraud screening complete for payment {}: totalScore={}", payment.getPaymentId(), totalScore);

        // Determine decision based on thresholds
        if (totalScore >= properties.getBlockThreshold()) {
            // BLOCK: Create alert and throw exception
            String reason = buildReason(ruleResults);
            createAlert(payment, AlertType.MANUAL_FLAG, totalScore, reason, ruleResults);

            FraudScreeningResult result = FraudScreeningResult.block(payment.getPaymentId(), totalScore, ruleResults, reason);
            throw new PaymentBlockedByFraudException(payment.getPaymentId(), reason, totalScore);

        } else if (totalScore >= properties.getApproveThreshold()) {
            // FLAG: Create alert but allow transaction to proceed
            String reason = buildReason(ruleResults);
            createAlert(payment, determinePrimaryAlertType(ruleResults), totalScore, reason, ruleResults);

            return FraudScreeningResult.flag(payment.getPaymentId(), totalScore, ruleResults, reason);

        } else {
            // APPROVE: Clean transaction
            return FraudScreeningResult.approve(payment.getPaymentId(), totalScore, ruleResults);
        }
    }

    /**
     * Create a fraud alert
     */
    @Transactional
    public FraudAlert createAlert(Payment payment, AlertType alertType, int riskScore, String reason, List<RuleResult> ruleResults) {
        FraudAlert alert = new FraudAlert(
            payment.getPaymentId(),
            payment.getFromAccount(),
            payment.getUserId(),
            alertType,
            riskScore,
            payment.getAmount(),
            reason
        );

        try {
            alert.setRuleDetails(objectMapper.writeValueAsString(ruleResults));
        } catch (Exception e) {
            logger.error("Failed to serialize rule details", e);
            alert.setRuleDetails("[]");
        }

        alert = alertRepository.save(alert);
        logger.info("Created fraud alert {} for payment {}: type={}, score={}", alert.getId(), payment.getPaymentId(), alertType, riskScore);

        return alert;
    }

    private String buildReason(List<RuleResult> results) {
        return results.stream()
            .filter(RuleResult::triggered)
            .map(r -> r.ruleName() + ": " + r.reason())
            .reduce((a, b) -> a + "; " + b)
            .orElse("Unknown risk factors");
    }

    private AlertType determinePrimaryAlertType(List<RuleResult> results) {
        // Find the highest-scoring triggered rule and map to alert type
        return results.stream()
            .filter(RuleResult::triggered)
            .max((a, b) -> Integer.compare(a.riskScore(), b.riskScore()))
            .map(r -> switch (r.ruleName()) {
                case "VelocityRule" -> AlertType.VELOCITY_BREACH;
                case "AmountAnomalyRule" -> AlertType.AMOUNT_ANOMALY;
                case "UnusualDestinationRule" -> AlertType.UNUSUAL_DESTINATION;
                case "UnusualTimeRule" -> AlertType.UNUSUAL_TIME;
                case "BlacklistRule" -> AlertType.BLACKLISTED_ACCOUNT;
                default -> AlertType.MANUAL_FLAG;
            })
            .orElse(AlertType.MANUAL_FLAG);
    }
}
