package com.fintech.schedulerservice.dto;

import com.fintech.schedulerservice.entity.JobType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for creating new scheduled jobs
 */
public record JobRequest(

        @NotBlank(message = "Job name is required")
        @Size(max = 255, message = "Job name cannot exceed 255 characters")
        String jobName,

        @NotNull(message = "Job type is required")
        JobType jobType,

        @NotNull(message = "Scheduled time is required")
        LocalDateTime scheduledTime,

        @Size(max = 1000, message = "Job description cannot exceed 1000 characters")
        String description,

        @NotBlank(message = "Created by is required")
        @Size(max = 100, message = "Created by cannot exceed 100 characters")
        String createdBy,

        Map<String, Object> jobData,

        Integer maxRetries,

        Integer retryDelaySeconds,

        @Size(max = 50, message = "Priority cannot exceed 50 characters")
        String priority
) {


}