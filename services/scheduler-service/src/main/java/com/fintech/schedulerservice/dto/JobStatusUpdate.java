package com.fintech.schedulerservice.dto;


import com.fintech.schedulerservice.entity.JobStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * DTO for job status updates
 */
public class JobStatusUpdate {

    private String jobId;
    @NotNull
    private JobStatus jobStatus;
    private String executionResult;
    private String errorMessage;
    private LocalDateTime executionTime;
    private String updatedBy;

    public JobStatusUpdate(String jobId, JobStatus jobStatus, String executionResult, String errorMessage, LocalDateTime executionTime, String updatedBy) {
        this.jobId = jobId;
        this.jobStatus = jobStatus;
        this.executionResult = executionResult;
        this.errorMessage = errorMessage;
        this.executionTime = executionTime;
        this.updatedBy = updatedBy;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public JobStatus getJobStatus() {
        return jobStatus;
    }

    public void setJobStatus(JobStatus jobStatus) {
        this.jobStatus = jobStatus;
    }

    public String getExecutionResult() {
        return executionResult;
    }

    public void setExecutionResult(String executionResult) {
        this.executionResult = executionResult;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(LocalDateTime executionTime) {
        this.executionTime = executionTime;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}