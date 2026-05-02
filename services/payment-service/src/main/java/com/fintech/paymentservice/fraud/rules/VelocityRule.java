package com.fintech.paymentservice.fraud.rules;

import com.fintech.paymentservice.entity.Payment;
import com.fintech.paymentservice.fraud.config.FraudDetectionProperties;
import com.fintech.paymentservice.fraud.model.RuleResult;
import com.fintech.paymentservice.repository.PaymentRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

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

        // Count recent transactions from same account
        Instant windowStart = Instant.now().minus(windowMinutes, ChronoUnit.MINUTES);
        List<Payment> recentPayments = paymentRepository.findByFromAccountOrToAccount(
            payment.getFromAccount(), payment.getFromAccount());

        long recentCount = recentPayments.stream()
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
