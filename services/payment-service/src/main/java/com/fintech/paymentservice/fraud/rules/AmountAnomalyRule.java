package com.fintech.paymentservice.fraud.rules;

import com.fintech.paymentservice.entity.Payment;
import com.fintech.paymentservice.fraud.config.FraudDetectionProperties;
import com.fintech.paymentservice.fraud.model.RuleResult;
import com.fintech.paymentservice.repository.PaymentRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AmountAnomalyRule implements FraudRule {

    private final PaymentRepository paymentRepository;
    private final FraudDetectionProperties properties;

    public AmountAnomalyRule(PaymentRepository paymentRepository, FraudDetectionProperties properties) {
        this.paymentRepository = paymentRepository;
        this.properties = properties;
    }

    @Override
    public RuleResult evaluate(Payment payment) {
        BigDecimal multiplier = properties.getAmount().getAnomalyMultiplier();

        // Only look at last 90 days, limit to 1000 records to prevent OOM
        Instant windowStart = Instant.now().minus(90, ChronoUnit.DAYS);
        List<Payment> userPayments = paymentRepository.findByFromAccountOrToAccount(
            payment.getFromAccount(), payment.getFromAccount()).stream()
            .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(windowStart))
            .limit(1000)
            .collect(Collectors.toList());

        if (userPayments.size() < 3) {
            // Not enough history to determine anomaly
            return RuleResult.pass(getName());
        }

        BigDecimal totalAmount = userPayments.stream()
            .map(Payment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageAmount = totalAmount.divide(BigDecimal.valueOf(userPayments.size()), 4, java.math.RoundingMode.HALF_UP);
        BigDecimal threshold = averageAmount.multiply(multiplier);

        if (payment.getAmount().compareTo(threshold) > 0) {
            return RuleResult.triggered(getName(), properties.getAmount().getRiskScore(),
                "Amount anomaly: " + payment.getAmount() + " exceeds " + multiplier + "x average of " + averageAmount);
        }

        return RuleResult.pass(getName());
    }

    @Override
    public String getName() {
        return "AmountAnomalyRule";
    }
}
