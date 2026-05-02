package com.fintech.paymentservice.fraud.rules;

import com.fintech.paymentservice.entity.Payment;
import com.fintech.paymentservice.fraud.config.FraudDetectionProperties;
import com.fintech.paymentservice.fraud.model.RuleResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BlacklistRule implements FraudRule {

    private final FraudDetectionProperties properties;

    public BlacklistRule(FraudDetectionProperties properties) {
        this.properties = properties;
    }

    @Override
    public RuleResult evaluate(Payment payment) {
        List<String> blacklistedAccounts = properties.getBlacklist().getAccounts();

        if (blacklistedAccounts == null || blacklistedAccounts.isEmpty()) {
            return RuleResult.pass(getName());
        }

        boolean fromBlacklisted = blacklistedAccounts.contains(payment.getFromAccount());
        boolean toBlacklisted = blacklistedAccounts.contains(payment.getToAccount());

        if (fromBlacklisted || toBlacklisted) {
            String flaggedAccount = fromBlacklisted ? payment.getFromAccount() : payment.getToAccount();
            return RuleResult.triggered(getName(), properties.getBlacklist().getRiskScore(),
                "Blacklisted account detected: " + flaggedAccount);
        }

        return RuleResult.pass(getName());
    }

    @Override
    public String getName() {
        return "BlacklistRule";
    }
}
