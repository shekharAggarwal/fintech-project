package com.fintech.paymentservice.dto.response;

import com.fintech.paymentservice.model.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentInitiatedResponse(
        String paymentId,
        String fromAccount,
        String toAccount,
        BigDecimal amount,
        String description,
        PaymentStatus status,
        Instant createdAt,
        String message
) {
}