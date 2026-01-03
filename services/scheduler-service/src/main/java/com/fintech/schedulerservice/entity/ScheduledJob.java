package com.fintech.schedulerservice.entity;

import com.fintech.security.annotation.FieldAccessControl;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "scheduled_jobs", indexes = {
    @Index(name = "idx_job_status", columnList = "status"),
    @Index(name = "idx_job_type", columnList = "job_type"),
    @Index(name = "idx_job_next_run", columnList = "next_run_time"),
    @Index(name = "idx_job_created", columnList = "created_at"),
    @Index(name = "idx_job_quartz_name", columnList = "quartz_job_name"),
    @Index(name = "idx_job_quartz_group", columnList = "quartz_job_group")
})
public class ScheduledJob {
    
    @Id
    @Column(name = "job_id", nullable = false, length = 20)
    @FieldAccessControl(resourceType = "job", fieldName = "jobId")
    private String jobId;

    @Column(name = "job_name", nullable = false, length = 100)
    @FieldAccessControl(resourceType = "job", fieldName = "jobName")
    private String jobName;

    @Column(name = "job_description", length = 500)
    @FieldAccessControl(resourceType = "job", fieldName = "jobDescription")
    private String jobDescription;

    @Column(name = "job_type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    @FieldAccessControl(resourceType = "job", fieldName = "jobType")
    private JobType jobType;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @FieldAccessControl(resourceType = "job", fieldName = "status")
    private JobStatus status = JobStatus.SCHEDULED;

    @Column(name = "cron_expression", length = 100)
    @FieldAccessControl(resourceType = "job", fieldName = "cronExpression")
    private String cronExpression;

    @Column(name = "quartz_job_name", nullable = false, length = 100)
    @FieldAccessControl(resourceType = "job", fieldName = "quartzJobName")
    private String quartzJobName;

    @Column(name = "quartz_job_group", nullable = false, length = 100)
    @FieldAccessControl(resourceType = "job", fieldName = "quartzJobGroup")
    private String quartzJobGroup;

    @Column(name = "job_class", nullable = false, length = 255)
    @FieldAccessControl(resourceType = "job", fieldName = "jobClass")
    private String jobClass;

    @Column(name = "job_data", columnDefinition = "TEXT")
    @FieldAccessControl(resourceType = "job", fieldName = "jobData")
    private String jobData; // JSON data for job parameters

    @Column(name = "next_run_time")
    @FieldAccessControl(resourceType = "job", fieldName = "nextRunTime")
    private Instant nextRunTime;

    @Column(name = "last_run_time")
    @FieldAccessControl(resourceType = "job", fieldName = "lastRunTime")
    private Instant lastRunTime;

    @Column(name = "scheduled_time")
    @FieldAccessControl(resourceType = "job", fieldName = "scheduledTime")
    private Instant scheduledTime;

    @Column(name = "actual_execution_time")
    @FieldAccessControl(resourceType = "job", fieldName = "actualExecutionTime")
    private Instant actualExecutionTime;

    @Column(name = "execution_count", nullable = false)
    @FieldAccessControl(resourceType = "job", fieldName = "executionCount")
    private Long executionCount = 0L;

    @Column(name = "failure_count", nullable = false)
    @FieldAccessControl(resourceType = "job", fieldName = "failureCount")
    private Integer failureCount = 0;

    @Column(name = "retry_count", nullable = false)
    @FieldAccessControl(resourceType = "job", fieldName = "retryCount")
    private Integer retryCount = 0;

    @Column(name = "retry_delay_seconds", nullable = false)
    @FieldAccessControl(resourceType = "job", fieldName = "retryDelaySeconds")
    private Integer retryDelaySeconds = 60;

    @Column(name = "execution_result", length = 2000)
    @FieldAccessControl(resourceType = "job", fieldName = "executionResult")
    private String executionResult;

    @Column(name = "error_message", length = 2000)
    @FieldAccessControl(resourceType = "job", fieldName = "errorMessage")
    private String errorMessage;

    @Column(name = "priority", length = 50)
    @FieldAccessControl(resourceType = "job", fieldName = "priority")
    private String priority = "NORMAL";

    @Column(name = "last_updated_by", length = 50)
    @FieldAccessControl(resourceType = "job", fieldName = "lastUpdatedBy")
    private String lastUpdatedBy;

    @Column(name = "max_retries", nullable = false)
    @FieldAccessControl(resourceType = "job", fieldName = "maxRetries")
    private Integer maxRetries = 3;

    @Column(name = "last_error_message", length = 1000)
    @FieldAccessControl(resourceType = "job", fieldName = "lastErrorMessage")
    private String lastErrorMessage;

    @Column(name = "created_by", nullable = false, length = 50)
    @FieldAccessControl(resourceType = "job", fieldName = "createdBy", sensitive = true)
    private String createdBy;

    @Column(name = "is_active", nullable = false)
    @FieldAccessControl(resourceType = "job", fieldName = "isActive")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @FieldAccessControl(resourceType = "job", fieldName = "createdAt")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @FieldAccessControl(resourceType = "job", fieldName = "updatedAt")
    private Instant updatedAt;

    // Constructors
    public ScheduledJob() {}

    public ScheduledJob(String jobId, String jobName, JobType jobType, String quartzJobName, 
                       String quartzJobGroup, String jobClass, String createdBy) {
        this.jobId = jobId;
        this.jobName = jobName;
        this.jobType = jobType;
        this.quartzJobName = quartzJobName;
        this.quartzJobGroup = quartzJobGroup;
        this.jobClass = jobClass;
        this.createdBy = createdBy;
    }

    // Constructor that matches service usage
    public ScheduledJob(String jobId, String jobName, JobType jobType, JobStatus status, 
                       Instant scheduledTime, String description, String createdBy, 
                       String lastUpdatedBy, String jobData, Integer retryCount, 
                       Integer maxRetries, Integer retryDelaySeconds, String priority) {
        this.jobId = jobId;
        this.jobName = jobName;
        this.jobType = jobType;
        this.status = status;
        this.scheduledTime = scheduledTime;
        this.jobDescription = description;
        this.createdBy = createdBy;
        this.lastUpdatedBy = lastUpdatedBy;
        this.jobData = jobData;
        this.retryCount = retryCount;
        this.maxRetries = maxRetries;
        this.retryDelaySeconds = retryDelaySeconds;
        this.priority = priority;
        
        // Set default values for Quartz fields
        this.quartzJobName = jobId + "_job";
        this.quartzJobGroup = "DEFAULT";
        this.jobClass = "com.fintech.schedulerservice.job.DefaultJob";
    }

    // Getters and Setters
    public String getJobId() { return jobId; }
    public void setJobId(String jobId) { this.jobId = jobId; }

    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }

    public String getJobDescription() { return jobDescription; }
    public void setJobDescription(String jobDescription) { this.jobDescription = jobDescription; }

    public JobType getJobType() { return jobType; }
    public void setJobType(JobType jobType) { this.jobType = jobType; }

    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }

    public String getCronExpression() { return cronExpression; }
    public void setCronExpression(String cronExpression) { this.cronExpression = cronExpression; }

    public String getQuartzJobName() { return quartzJobName; }
    public void setQuartzJobName(String quartzJobName) { this.quartzJobName = quartzJobName; }

    public String getQuartzJobGroup() { return quartzJobGroup; }
    public void setQuartzJobGroup(String quartzJobGroup) { this.quartzJobGroup = quartzJobGroup; }

    public String getJobClass() { return jobClass; }
    public void setJobClass(String jobClass) { this.jobClass = jobClass; }

    public String getJobData() { return jobData; }
    public void setJobData(String jobData) { this.jobData = jobData; }

    public Instant getNextRunTime() { return nextRunTime; }
    public void setNextRunTime(Instant nextRunTime) { this.nextRunTime = nextRunTime; }

    public Instant getLastRunTime() { return lastRunTime; }
    public void setLastRunTime(Instant lastRunTime) { this.lastRunTime = lastRunTime; }

    public Long getExecutionCount() { return executionCount; }
    public void setExecutionCount(Long executionCount) { this.executionCount = executionCount; }

    public Integer getFailureCount() { return failureCount; }
    public void setFailureCount(Integer failureCount) { this.failureCount = failureCount; }

    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }

    public String getLastErrorMessage() { return lastErrorMessage; }
    public void setLastErrorMessage(String lastErrorMessage) { this.lastErrorMessage = lastErrorMessage; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    // Additional getters and setters for new fields
    public Instant getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(Instant scheduledTime) { this.scheduledTime = scheduledTime; }

    public Instant getActualExecutionTime() { return actualExecutionTime; }
    public void setActualExecutionTime(Instant actualExecutionTime) { this.actualExecutionTime = actualExecutionTime; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public Integer getRetryDelaySeconds() { return retryDelaySeconds; }
    public void setRetryDelaySeconds(Integer retryDelaySeconds) { this.retryDelaySeconds = retryDelaySeconds; }

    public String getExecutionResult() { return executionResult; }
    public void setExecutionResult(String executionResult) { this.executionResult = executionResult; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getLastUpdatedBy() { return lastUpdatedBy; }
    public void setLastUpdatedBy(String lastUpdatedBy) { this.lastUpdatedBy = lastUpdatedBy; }

    // Convenience methods for LocalDateTime conversion
    public void setScheduledTime(java.time.LocalDateTime scheduledTime) {
        this.scheduledTime = scheduledTime != null ? scheduledTime.atZone(java.time.ZoneId.systemDefault()).toInstant() : null;
    }

    public void setActualExecutionTime(java.time.LocalDateTime actualExecutionTime) {
        this.actualExecutionTime = actualExecutionTime != null ? actualExecutionTime.atZone(java.time.ZoneId.systemDefault()).toInstant() : null;
    }

    public JobStatus getJobStatus() { return status; }
    public void setJobStatus(JobStatus jobStatus) { this.status = jobStatus; }

    public String getDescription() { return jobDescription; }

    // Helper methods
    public boolean canRetry() {
        return status == JobStatus.FAILED && failureCount < maxRetries;
    }

    public void incrementExecutionCount() {
        this.executionCount++;
    }

    public void incrementFailureCount() {
        this.failureCount++;
    }

    public boolean isOverdue() {
        return nextRunTime != null && nextRunTime.isBefore(Instant.now());
    }
}