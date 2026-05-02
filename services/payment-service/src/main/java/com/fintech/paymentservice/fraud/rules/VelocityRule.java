package com.fintech.paymentservice.fraud.rules;

import com.fintech.paymentservice.entity.Payment;
import com.fintech.paymentservice.fraud.config.FraudDetectionProperties;
import com.fintech.paymentservice.fraud.model.RuleResult;
import com.fintech.paymentservice.repository.PaymentRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class VelocityRule implements FraudRule {

    private final PaymentRepository paymentRepository;
    private final FraudDetectionProperties properties;

    public VelocityRule(PaymentRepository paymentRepository, FraudDetectionProperties properties) {
        this.paymentRepository = paymentRepository;
        this.properties = properties;
    }

    @Override
    public RuleResult evaluate(Payment payment) {
        int windowMinutes = properties.getVelocity().getWindowMinutes();
        int maxTransactions = properties.getVelocity().getMaxTransactions();

        // Only look at last 24 hours max, use time-window filtering
        Instant windowStart = Instant.now().minus(Math.min(windowMinutes, 1440), ChronoUnit.MINUTES);
        long recentCount = paymentRepository.findByFromAccountOrToAccount(
            payment.getFromAccount(), payment.getFromAccount()).stream()
            .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(windowStart))
            .count();

        if (recentCount >= maxTransactions) {
            return RuleResult.triggered(getName(), properties.getVelocity().getRiskScore(),
                "High velocity: " + recentCount + " transactions in " + windowMinutes + " minutes (max: " + maxTransactions + ")");
        }

        return RuleResult.pass(getName());
    }

    @Override
    public String getName() {
        return "VelocityRule";
    }
}
