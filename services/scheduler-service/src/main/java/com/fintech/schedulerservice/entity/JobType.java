package com.fintech.schedulerservice.entity;

/**
 * Enumeration for job types
 */
public enum JobType {
    PAYMENT_RETRY,          // Retry failed payment transactions
    TRANSACTION_RETRY,      // Retry failed transaction processing
    NOTIFICATION_SCHEDULED, // Send scheduled notifications
    NOTIFICATION_REMINDER,  // Send reminder notifications
    DATA_CLEANUP,           // Clean up old data
    ACCOUNT_CLEANUP,        // Clean up old accounts
    REPORT_GENERATION,      // Generate scheduled reports
    HEALTH_CHECK,           // Periodic health checks
    RECONCILIATION,         // Data reconciliation jobs
    DATA_SYNC,              // Data synchronization jobs
    BULK_PROCESSING,        // Bulk data processing
    BATCH_PROCESSING        // Batch processing jobs
}