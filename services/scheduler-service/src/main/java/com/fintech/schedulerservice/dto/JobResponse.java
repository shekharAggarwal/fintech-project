package com.fintech.schedulerservice.dto;

import com.fintech.schedulerservice.model.JobStatus;
import com.fintech.schedulerservice.model.JobType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for job response data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobResponse {

    private String jobId;
    private String jobName;
    private JobType jobType;
    private JobStatus jobStatus;
    private LocalDateTime scheduledTime;
    private LocalDateTime actualExecutionTime;
    private String description;
    private String createdBy;
    private String lastUpdatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Map<String, Object> jobData;
    private String executionResult;
    private String errorMessage;
    private Integer retryCount;
    private Integer maxRetries;
    private Integer retryDelaySeconds;
    private String priority;
}