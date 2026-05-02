package com.fintech.paymentservice.fraud.model;

public record RuleResult(
    String ruleName,
    boolean triggered,
    int riskScore,
    String reason
) {
    public static RuleResult pass(String ruleName) {
        return new RuleResult(ruleName, false, 0, null);
    }

    public static RuleResult triggered(String ruleName, int riskScore, String reason) {
        return new RuleResult(ruleName, true, riskScore, reason);
    }
}
