package com.fintech.retryservice.model;

/**
 * Enumeration for retry status
 */
public enum RetryStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED,
    MAX_RETRIES_EXCEEDED
}