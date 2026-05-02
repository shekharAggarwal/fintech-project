package com.fintech.paymentservice.fraud.rules;

import com.fintech.paymentservice.entity.Payment;
import com.fintech.paymentservice.fraud.config.FraudDetectionProperties;
import com.fintech.paymentservice.fraud.model.RuleResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Component
public class UnusualTimeRule implements FraudRule {

    private final FraudDetectionProperties properties;

    public UnusualTimeRule(FraudDetectionProperties properties) {
        this.properties = properties;
    }

    @Override
    public RuleResult evaluate(Payment payment) {
        int startHour = properties.getTime().getSuspiciousStartHour();
        int endHour = properties.getTime().getSuspiciousEndHour();

        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        int currentHour = now.getHour();

        boolean isSuspiciousTime;
        if (startHour < endHour) {
            isSuspiciousTime = currentHour >= startHour && currentHour < endHour;
        } else {
            // Handles wrap-around (e.g., 23:00 to 05:00)
            isSuspiciousTime = currentHour >= startHour || currentHour < endHour;
        }

        if (isSuspiciousTime) {
            return RuleResult.triggered(getName(), properties.getTime().getRiskScore(),
                "Transaction at unusual time: " + currentHour + ":00 UTC (suspicious window: " + startHour + ":00-" + endHour + ":00)");
        }

        return RuleResult.pass(getName());
    }

    @Override
    public String getName() {
        return "UnusualTimeRule";
    }
}
