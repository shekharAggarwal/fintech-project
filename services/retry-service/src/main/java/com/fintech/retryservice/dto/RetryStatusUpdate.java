package com.fintech.retryservice.dto;

import com.fintech.retryservice.model.RetryStatus;

import java.time.LocalDateTime;

/**
 * DTO for retry status updates
 */
public class RetryStatusUpdate {

    private String retryId;
    private RetryStatus retryStatus;
    private String errorMessage;
    private String lastErrorCode;
    private LocalDateTime completedAt;
    private String updatedBy;
}