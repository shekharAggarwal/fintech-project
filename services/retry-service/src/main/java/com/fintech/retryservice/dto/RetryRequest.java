package com.fintech.retryservice.dto;

import com.fintech.retryservice.model.RetryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for creating new retry attempts
 */
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

    // --- Getters and Setters ---

    public String getOriginalId() { return originalId; }
    public void setOriginalId(String originalId) { this.originalId = originalId; }

    public RetryType getRetryType() { return retryType; }
    public void setRetryType(RetryType retryType) { this.retryType = retryType; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getEndpointUrl() { return endpointUrl; }
    public void setEndpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Map<String, String> getRetryData() { return retryData; }
    public void setRetryData(Map<String, String> retryData) { this.retryData = retryData; }

    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }

    public Integer getRetryDelaySeconds() { return retryDelaySeconds; }
    public void setRetryDelaySeconds(Integer retryDelaySeconds) { this.retryDelaySeconds = retryDelaySeconds; }

    public LocalDateTime getNextRetryTime() { return nextRetryTime; }
    public void setNextRetryTime(LocalDateTime nextRetryTime) { this.nextRetryTime = nextRetryTime; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
}