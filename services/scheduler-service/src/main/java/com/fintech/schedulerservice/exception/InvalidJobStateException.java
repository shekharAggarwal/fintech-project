package com.fintech.schedulerservice.exception;

import com.fintech.schedulerservice.entity.JobStatus;

/**
 * Exception thrown when a job operation is invalid for the current job state
 */
public class InvalidJobStateException extends RuntimeException {

    private final String jobId;
    private final JobStatus currentStatus;

    public InvalidJobStateException(String jobId, JobStatus currentStatus, String operation) {
        super(String.format("Cannot %s job '%s' in status: %s", operation, jobId, currentStatus));
        this.jobId = jobId;
        this.currentStatus = currentStatus;
    }

    public InvalidJobStateException(String message) {
        super(message);
        this.jobId = null;
        this.currentStatus = null;
    }

    public String getJobId() {
        return jobId;
    }

    public JobStatus getCurrentStatus() {
        return currentStatus;
    }
}
