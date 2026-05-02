package com.fintech.paymentservice.saga;

public enum SagaStatus {
    IN_PROGRESS,
    COMPLETED,
    COMPENSATING,
    FAILED,
    COMPENSATED
}
