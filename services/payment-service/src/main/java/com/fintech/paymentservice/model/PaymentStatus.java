package com.fintech.paymentservice.model;

public enum PaymentStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    AUTHORIZED,
    PENDING_VERIFICATION,
    STUCK
}
