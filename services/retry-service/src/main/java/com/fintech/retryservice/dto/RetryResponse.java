package com.fintech.retryservice.dto;

import com.fintech.retryservice.model.RetryStatus;
import com.fintech.retryservice.model.RetryType;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO for retry response data
 */
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

    // --- Getters and Setters ---

    public String getRetryId() { return retryId; }
    public void setRetryId(String retryId) { this.retryId = retryId; }

    public String getOriginalId() { return originalId; }
    public void setOriginalId(String originalId) { this.originalId = originalId; }

    public RetryType getRetryType() { return retryType; }
    public void setRetryType(RetryType retryType) { this.retryType = retryType; }

    public RetryStatus getRetryStatus() { return retryStatus; }
    public void setRetryStatus(RetryStatus retryStatus) { this.retryStatus = retryStatus; }

    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }

    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }

    public LocalDateTime getNextRetryTime() { return nextRetryTime; }
    public void setNextRetryTime(LocalDateTime nextRetryTime) { this.nextRetryTime = nextRetryTime; }

    public Integer getRetryDelaySeconds() { return retryDelaySeconds; }
    public void setRetryDelaySeconds(Integer retryDelaySeconds) { this.retryDelaySeconds = retryDelaySeconds; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getEndpointUrl() { return endpointUrl; }
    public void setEndpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; }

    public Map<String, String> getRetryData() { return retryData; }
    public void setRetryData(Map<String, String> retryData) { this.retryData = retryData; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getLastErrorCode() { return lastErrorCode; }
    public void setLastErrorCode(String lastErrorCode) { this.lastErrorCode = lastErrorCode; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public String getLastUpdatedBy() { return lastUpdatedBy; }
    public void setLastUpdatedBy(String lastUpdatedBy) { this.lastUpdatedBy = lastUpdatedBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getLastRetryTime() { return lastRetryTime; }
    public void setLastRetryTime(LocalDateTime lastRetryTime) { this.lastRetryTime = lastRetryTime; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    /**
     * Build a RetryResponse from a RetryAttempt entity
     */
    public static RetryResponse fromEntity(com.fintech.retryservice.model.RetryAttempt entity) {
        RetryResponse response = new RetryResponse();
        response.setRetryId(entity.getRetryId());
        response.setOriginalId(entity.getOriginalId());
        response.setRetryType(entity.getRetryType());
        response.setRetryStatus(entity.getRetryStatus());
        response.setRetryCount(entity.getRetryCount());
        response.setMaxRetries(entity.getMaxRetries());
        response.setNextRetryTime(entity.getNextRetryTime());
        response.setRetryDelaySeconds(entity.getRetryDelaySeconds());
        response.setPriority(entity.getPriority());
        response.setServiceName(entity.getServiceName());
        response.setEndpointUrl(entity.getEndpointUrl());
        response.setRetryData(entity.getRetryData());
        response.setErrorMessage(entity.getErrorMessage());
        response.setLastErrorCode(entity.getLastErrorCode());
        response.setCreatedBy(entity.getCreatedBy());
        response.setLastUpdatedBy(entity.getLastUpdatedBy());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        response.setLastRetryTime(entity.getLastRetryTime());
        response.setCompletedAt(entity.getCompletedAt());
        return response;
    }
}