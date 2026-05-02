package com.fintech.paymentservice.fraud.model;

public enum AlertType {
    VELOCITY_BREACH,
    AMOUNT_ANOMALY,
    UNUSUAL_DESTINATION,
    UNUSUAL_TIME,
    BLACKLISTED_ACCOUNT,
    MANUAL_FLAG
}
