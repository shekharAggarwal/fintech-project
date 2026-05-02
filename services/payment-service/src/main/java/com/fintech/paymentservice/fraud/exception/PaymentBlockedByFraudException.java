package com.fintech.paymentservice.fraud.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class PaymentBlockedByFraudException extends RuntimeException {

    private final String paymentId;
    private final int riskScore;

    public PaymentBlockedByFraudException(String paymentId, String reason, int riskScore) {
        super("Payment " + paymentId + " blocked by fraud detection: " + reason);
        this.paymentId = paymentId;
        this.riskScore = riskScore;
    }

    public String getPaymentId() { return paymentId; }
    public int getRiskScore() { return riskScore; }
}
