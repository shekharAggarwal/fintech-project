package com.fintech.schedulerservice.exception;

/**
 * Exception thrown when job scheduling fails
 */
public class JobSchedulingException extends RuntimeException {

    private final String jobId;

    public JobSchedulingException(String message) {
        super(message);
        this.jobId = null;
    }

    public JobSchedulingException(String message, String jobId) {
        super(message);
        this.jobId = jobId;
    }

    public JobSchedulingException(String message, String jobId, Throwable cause) {
        super(message, cause);
        this.jobId = jobId;
    }

    public JobSchedulingException(String message, Throwable cause) {
        super(message, cause);
        this.jobId = null;
    }

    public String getJobId() {
        return jobId;
    }
}
