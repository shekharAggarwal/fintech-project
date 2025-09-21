package com.fintech.retryservice.dto;

import com.fintech.retryservice.model.RetryType;
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
 * DTO for creating new retry attempts
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetryRequest {

    @NotBlank(message = "Original ID is required")
    @Size(max = 50, message = "Original ID cannot exceed 50 characters")
    private String originalId;

    @NotNull(message = "Retry type is required")
    private RetryType retryType;

    @NotBlank(message = "Service name is required")
    @Size(max = 100, message = "Service name cannot exceed 100 characters")
    private String serviceName;

    @Size(max = 500, message = "Endpoint URL cannot exceed 500 characters")
    private String endpointUrl;

    @NotBlank(message = "Created by is required")
    @Size(max = 100, message = "Created by cannot exceed 100 characters")
    private String createdBy;

    private Map<String, String> retryData;

    private Integer maxRetries;

    private Integer retryDelaySeconds;

    private LocalDateTime nextRetryTime;

    @Size(max = 20, message = "Priority cannot exceed 20 characters")
    private String priority;
}