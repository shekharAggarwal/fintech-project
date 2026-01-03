package com.fintech.schedulerservice.dto;

import com.fintech.schedulerservice.entity.JobType;
import com.fintech.schedulerservice.entity.JobStatus;

import java.time.Instant;

/**
 * DTO for job response data
 */
public class JobResponse {

    private String jobId;
    private String jobName;
    private JobType jobType;
    private JobStatus jobStatus;
    private Instant scheduledTime;
    private Instant actualExecutionTime;
    private String description;
    private String createdBy;
    private String lastUpdatedBy;
    private Instant createdAt;
    private Instant updatedAt;
    private String jobData;
    private String executionResult;
    private String errorMessage;
    private Integer retryCount;
    private Integer maxRetries;
    private Integer retryDelaySeconds;
    private String priority;

    // Default constructor
    public JobResponse() {}

    // Constructor
    public JobResponse(String jobId, String jobName, JobType jobType, JobStatus jobStatus,
                      Instant scheduledTime, Instant actualExecutionTime, String description,
                      String createdBy, String lastUpdatedBy, Instant createdAt, Instant updatedAt,
                      String jobData, String executionResult, String errorMessage,
                      Integer retryCount, Integer maxRetries, Integer retryDelaySeconds, String priority) {
        this.jobId = jobId;
        this.jobName = jobName;
        this.jobType = jobType;
        this.jobStatus = jobStatus;
        this.scheduledTime = scheduledTime;
        this.actualExecutionTime = actualExecutionTime;
        this.description = description;
        this.createdBy = createdBy;
        this.lastUpdatedBy = lastUpdatedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.jobData = jobData;
        this.executionResult = executionResult;
        this.errorMessage = errorMessage;
        this.retryCount = retryCount;
        this.maxRetries = maxRetries;
        this.retryDelaySeconds = retryDelaySeconds;
        this.priority = priority;
    }

    // Getters and Setters
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }

    public JobType getJobType() { return jobType; }
    public void setJobType(JobType jobType) { this.jobType = jobType; }

    public JobStatus getJobStatus() { return jobStatus; }
    public void setJobStatus(JobStatus jobStatus) { this.jobStatus = jobStatus; }

    public Instant getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(Instant scheduledTime) { this.scheduledTime = scheduledTime; }

    public Instant getActualExecutionTime() { return actualExecutionTime; }
    public void setActualExecutionTime(Instant actualExecutionTime) { this.actualExecutionTime = actualExecutionTime; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getLastUpdatedBy() { return lastUpdatedBy; }
    public void setLastUpdatedBy(String lastUpdatedBy) { this.lastUpdatedBy = lastUpdatedBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getJobData() { return jobData; }
    public void setJobData(String jobData) { this.jobData = jobData; }

    public String getExecutionResult() { return executionResult; }
    public void setExecutionResult(String executionResult) { this.executionResult = executionResult; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }

    public Integer getRetryDelaySeconds() { return retryDelaySeconds; }
    public void setRetryDelaySeconds(Integer retryDelaySeconds) { this.retryDelaySeconds = retryDelaySeconds; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    // Builder pattern
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String jobId;
        private String jobName;
        private JobType jobType;
        private JobStatus jobStatus;
        private Instant scheduledTime;
        private Instant actualExecutionTime;
        private String description;
        private String createdBy;
        private String lastUpdatedBy;
        private Instant createdAt;
        private Instant updatedAt;
        private String jobData;
        private String executionResult;
        private String errorMessage;
        private Integer retryCount;
        private Integer maxRetries;
        private Integer retryDelaySeconds;
        private String priority;

        public Builder jobId(String jobId) { this.jobId = jobId; return this; }
        public Builder jobName(String jobName) { this.jobName = jobName; return this; }
        public Builder jobType(JobType jobType) { this.jobType = jobType; return this; }
        public Builder jobStatus(JobStatus jobStatus) { this.jobStatus = jobStatus; return this; }
        public Builder scheduledTime(Instant scheduledTime) { this.scheduledTime = scheduledTime; return this; }
        public Builder actualExecutionTime(Instant actualExecutionTime) { this.actualExecutionTime = actualExecutionTime; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder createdBy(String createdBy) { this.createdBy = createdBy; return this; }
        public Builder lastUpdatedBy(String lastUpdatedBy) { this.lastUpdatedBy = lastUpdatedBy; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder jobData(String jobData) { this.jobData = jobData; return this; }
        public Builder executionResult(String executionResult) { this.executionResult = executionResult; return this; }
        public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public Builder retryCount(Integer retryCount) { this.retryCount = retryCount; return this; }
        public Builder maxRetries(Integer maxRetries) { this.maxRetries = maxRetries; return this; }
        public Builder retryDelaySeconds(Integer retryDelaySeconds) { this.retryDelaySeconds = retryDelaySeconds; return this; }
        public Builder priority(String priority) { this.priority = priority; return this; }

        public JobResponse build() {
            return new JobResponse(jobId, jobName, jobType, jobStatus, scheduledTime, actualExecutionTime,
                    description, createdBy, lastUpdatedBy, createdAt, updatedAt, jobData, executionResult,
                    errorMessage, retryCount, maxRetries, retryDelaySeconds, priority);
        }
    }
}