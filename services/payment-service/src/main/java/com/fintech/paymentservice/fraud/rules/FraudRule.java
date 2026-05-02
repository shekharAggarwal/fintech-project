package com.fintech.paymentservice.fraud.rules;

import com.fintech.paymentservice.entity.Payment;
import com.fintech.paymentservice.fraud.model.RuleResult;

/**
 * Interface for fraud detection rules
 */
public interface FraudRule {

    /**
     * Evaluate the rule against a payment
     */
    RuleResult evaluate(Payment payment);

    /**
     * Get the name of this rule
     */
    String getName();
}
