package com.fintech.retryservice.dto;

import com.fintech.retryservice.model.RetryStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * DTO for retry status updates
 */
public class RetryStatusUpdate {

    private String retryId;
    @NotNull
    private RetryStatus retryStatus;
    private String errorMessage;
    private String lastErrorCode;
    private LocalDateTime completedAt;
    @NotNull
    private String updatedBy;

    // --- Getters and Setters ---

    public String getRetryId() { return retryId; }
    public void setRetryId(String retryId) { this.retryId = retryId; }

    public RetryStatus getRetryStatus() { return retryStatus; }
    public void setRetryStatus(RetryStatus retryStatus) { this.retryStatus = retryStatus; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getLastErrorCode() { return lastErrorCode; }
    public void setLastErrorCode(String lastErrorCode) { this.lastErrorCode = lastErrorCode; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}