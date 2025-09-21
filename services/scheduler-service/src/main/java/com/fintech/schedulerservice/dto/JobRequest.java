package com.fintech.schedulerservice.dto;

import com.fintech.schedulerservice.model.JobType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for creating new scheduled jobs
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobRequest {

    @NotBlank(message = "Job name is required")
    @Size(max = 255, message = "Job name cannot exceed 255 characters")
    private String jobName;

    @NotNull(message = "Job type is required")
    private JobType jobType;

    @NotNull(message = "Scheduled time is required")
    private LocalDateTime scheduledTime;

    @Size(max = 1000, message = "Job description cannot exceed 1000 characters")
    private String description;

    @NotBlank(message = "Created by is required")
    @Size(max = 100, message = "Created by cannot exceed 100 characters")
    private String createdBy;

    private Map<String, Object> jobData;

    private Integer maxRetries;

    private Integer retryDelaySeconds;

    @Size(max = 50, message = "Priority cannot exceed 50 characters")
    private String priority;
}