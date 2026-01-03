package com.fintech.schedulerservice.entity;

/**
 * Enumeration for job statuses
 */
public enum JobStatus {
    SCHEDULED,      // Job is scheduled but not yet executed
    RUNNING,        // Job is currently executing
    IN_PROGRESS,    // Job is in progress (alias for RUNNING)
    COMPLETED,      // Job completed successfully
    FAILED,         // Job failed with error
    PAUSED,         // Job is paused
    CANCELLED,      // Job was cancelled
    RETRY_REQUIRED  // Job needs to be retried
}