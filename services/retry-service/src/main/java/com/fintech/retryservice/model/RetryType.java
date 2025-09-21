package com.fintech.retryservice.model;

/**
 * Enumeration for retry type
 */
public enum RetryType {
    PAYMENT_PROCESSING,
    TRANSACTION_PROCESSING,
    NOTIFICATION_DELIVERY,
    LEDGER_SYNC,
    EXTERNAL_API_CALL,
    DATA_VALIDATION,
    BATCH_PROCESSING
}