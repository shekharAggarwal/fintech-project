package com.fintech.paymentservice.fraud.rules;

import com.fintech.paymentservice.entity.Payment;
import com.fintech.paymentservice.fraud.config.FraudDetectionProperties;
import com.fintech.paymentservice.fraud.model.RuleResult;
import com.fintech.paymentservice.repository.PaymentRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

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
        double multiplier = properties.getAmount().getAnomalyMultiplier();

        // Calculate average transaction amount for this user
        List<Payment> userPayments = paymentRepository.findByFromAccountOrToAccount(
            payment.getFromAccount(), payment.getFromAccount());

        if (userPayments.size() < 3) {
            // Not enough history to determine anomaly
            return RuleResult.pass(getName());
        }

        BigDecimal totalAmount = userPayments.stream()
            .map(Payment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal averageAmount = totalAmount.divide(BigDecimal.valueOf(userPayments.size()), 4, java.math.RoundingMode.HALF_UP);
        BigDecimal threshold = averageAmount.multiply(BigDecimal.valueOf(multiplier));

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
