package com.fintech.paymentservice.fraud.model;

public enum AlertStatus {
    OPEN,
    INVESTIGATING,
    CONFIRMED_FRAUD,
    FALSE_POSITIVE,
    RESOLVED
}
