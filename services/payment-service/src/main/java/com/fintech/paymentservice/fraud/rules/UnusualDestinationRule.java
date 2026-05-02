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
public class UnusualDestinationRule implements FraudRule {

    private final PaymentRepository paymentRepository;
    private final FraudDetectionProperties properties;

    public UnusualDestinationRule(PaymentRepository paymentRepository, FraudDetectionProperties properties) {
        this.paymentRepository = paymentRepository;
        this.properties = properties;
    }

    @Override
    public RuleResult evaluate(Payment payment) {
        BigDecimal largeAmountThreshold = properties.getDestination().getLargeAmountThreshold();

        // Only look at last 30 days, limit to 100 records to prevent OOM
        Instant windowStart = Instant.now().minus(30, ChronoUnit.DAYS);
        List<Payment> previousPayments = paymentRepository.findByFromAccountOrToAccount(
            payment.getFromAccount(), payment.getFromAccount()).stream()
            .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(windowStart))
            .limit(100)
            .collect(Collectors.toList());

        boolean isFirstTimeDestination = previousPayments.stream()
            .noneMatch(p -> payment.getToAccount().equals(p.getToAccount()));

        boolean isLargeAmount = payment.getAmount().compareTo(largeAmountThreshold) > 0;

        if (isFirstTimeDestination && isLargeAmount) {
            return RuleResult.triggered(getName(), properties.getDestination().getRiskScore(),
                "First-time destination " + payment.getToAccount() + " with large amount: " + payment.getAmount());
        }

        return RuleResult.pass(getName());
    }

    @Override
    public String getName() {
        return "UnusualDestinationRule";
    }
}
