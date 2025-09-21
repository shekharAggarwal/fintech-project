package com.fintech.schedulerservice.entity;

/**
 * Enumeration for job types
 */
public enum JobType {
    PAYMENT_RETRY,          // Retry failed payment transactions
    TRANSACTION_RETRY,      // Retry failed transaction processing
    NOTIFICATION_SCHEDULED, // Send scheduled notifications
    DATA_CLEANUP,           // Clean up old data
    REPORT_GENERATION,      // Generate scheduled reports
    HEALTH_CHECK,           // Periodic health checks
    RECONCILIATION,         // Data reconciliation jobs
    BULK_PROCESSING         // Bulk data processing
}