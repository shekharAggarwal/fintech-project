package com.fintech.paymentservice.fraud.service;

import com.fintech.paymentservice.entity.Payment;
import com.fintech.paymentservice.fraud.model.RuleResult;
import com.fintech.paymentservice.fraud.rules.FraudRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class FraudRuleEngine {

    private static final Logger logger = LoggerFactory.getLogger(FraudRuleEngine.class);

    private final List<FraudRule> rules;

    public FraudRuleEngine(List<FraudRule> rules) {
        this.rules = rules;
        logger.info("Fraud rule engine initialized with {} rules", rules.size());
    }

    /**
     * Evaluate all rules against a payment and return aggregated results
     */
    public List<RuleResult> evaluateAll(Payment payment) {
        List<RuleResult> results = new ArrayList<>();

        for (FraudRule rule : rules) {
            try {
                RuleResult result = rule.evaluate(payment);
                results.add(result);

                if (result.triggered()) {
                    logger.info("Fraud rule {} triggered for payment {}: score={}, reason={}",
                        rule.getName(), payment.getPaymentId(), result.riskScore(), result.reason());
                }
            } catch (Exception e) {
                logger.error("Error evaluating fraud rule {} for payment {}: {}",
                    rule.getName(), payment.getPaymentId(), e.getMessage());
                // Don't fail the entire evaluation if one rule fails
                results.add(RuleResult.pass(rule.getName() + " (error)"));
            }
        }

        return results;
    }

    /**
     * Calculate total risk score from rule results
     */
    public int calculateTotalScore(List<RuleResult> results) {
        return results.stream()
            .filter(RuleResult::triggered)
            .mapToInt(RuleResult::riskScore)
            .sum();
    }
}
