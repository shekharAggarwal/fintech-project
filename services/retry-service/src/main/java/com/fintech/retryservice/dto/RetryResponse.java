package com.fintech.retryservice.dto;

import com.fintech.retryservice.model.RetryStatus;
import com.fintech.retryservice.model.RetryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for retry response data
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetryResponse {

    private String retryId;
    private String originalId;
    private RetryType retryType;
    private RetryStatus retryStatus;
    private Integer retryCount;
    private Integer maxRetries;
    private LocalDateTime nextRetryTime;
    private Integer retryDelaySeconds;
    private String priority;
    private String serviceName;
    private String endpointUrl;
    private Map<String, String> retryData;
    private String errorMessage;
    private String lastErrorCode;
    private String createdBy;
    private String lastUpdatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastRetryTime;
    private LocalDateTime completedAt;
}