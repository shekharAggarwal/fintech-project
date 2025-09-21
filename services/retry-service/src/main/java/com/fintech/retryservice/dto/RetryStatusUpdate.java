package com.fintech.retryservice.dto;

import com.fintech.retryservice.model.RetryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for retry status updates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetryStatusUpdate {

    private String retryId;
    private RetryStatus retryStatus;
    private String errorMessage;
    private String lastErrorCode;
    private LocalDateTime completedAt;
    private String updatedBy;
}