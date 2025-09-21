package com.fintech.transactionservice.entity;

public enum TransactionStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    RETRY_REQUIRED,
    CANCELLED
}
