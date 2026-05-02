package com.fintech.paymentservice.fraud.model;

import java.util.List;

public record FraudScreeningResult(
    FraudScreeningDecision decision,
    int totalRiskScore,
    List<RuleResult> ruleResults,
    String paymentId,
    String reason
) {
    public static FraudScreeningResult approve(String paymentId, int score, List<RuleResult> results) {
        return new FraudScreeningResult(FraudScreeningDecision.APPROVE, score, results, paymentId, null);
    }

    public static FraudScreeningResult flag(String paymentId, int score, List<RuleResult> results, String reason) {
        return new FraudScreeningResult(FraudScreeningDecision.FLAG, score, results, paymentId, reason);
    }

    public static FraudScreeningResult block(String paymentId, int score, List<RuleResult> results, String reason) {
        return new FraudScreeningResult(FraudScreeningDecision.BLOCK, score, results, paymentId, reason);
    }
}
