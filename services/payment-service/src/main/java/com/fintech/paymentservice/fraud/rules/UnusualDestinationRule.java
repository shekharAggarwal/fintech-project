package com.fintech.paymentservice.fraud.rules;

import com.fintech.paymentservice.entity.Payment;
import com.fintech.paymentservice.fraud.config.FraudDetectionProperties;
import com.fintech.paymentservice.fraud.model.RuleResult;
import com.fintech.paymentservice.repository.PaymentRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

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
        double largeAmountThreshold = properties.getDestination().getLargeAmountThreshold();

        // Check if this is a first-time destination combined with large amount
        List<Payment> previousPayments = paymentRepository.findByFromAccountOrToAccount(
            payment.getFromAccount(), payment.getFromAccount());

        boolean isFirstTimeDestination = previousPayments.stream()
            .noneMatch(p -> payment.getToAccount().equals(p.getToAccount()));

        boolean isLargeAmount = payment.getAmount().compareTo(BigDecimal.valueOf(largeAmountThreshold)) > 0;

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
