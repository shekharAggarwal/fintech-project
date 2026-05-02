package com.fintech.schedulerservice.exception;

/**
 * Exception thrown when a scheduled job is not found
 */
public class JobNotFoundException extends RuntimeException {

    private final String jobId;

    public JobNotFoundException(String jobId) {
        super("Job not found: " + jobId);
        this.jobId = jobId;
    }

    public JobNotFoundException(String jobId, Throwable cause) {
        super("Job not found: " + jobId, cause);
        this.jobId = jobId;
    }

    public String getJobId() {
        return jobId;
    }
}
