package com.fintech.schedulerservice.dto;

import com.fintech.schedulerservice.model.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for job status updates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobStatusUpdate {

    private String jobId;
    private JobStatus jobStatus;
    private String executionResult;
    private String errorMessage;
    private LocalDateTime executionTime;
    private String updatedBy;
}