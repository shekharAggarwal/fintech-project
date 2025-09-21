package com.fintech.retryservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entity class for retry attempts
 */
@Entity
@Table(name = "retry_attempts", indexes = {
    @Index(name = "idx_retry_status", columnList = "retry_status"),
    @Index(name = "idx_retry_type", columnList = "retry_type"),
    @Index(name = "idx_original_id", columnList = "original_id"),
    @Index(name = "idx_next_retry_time", columnList = "next_retry_time"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_status_retry_time", columnList = "retry_status, next_retry_time")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetryAttempt {

    @Id
    @Column(name = "retry_id", length = 50)
    private String retryId;

    @Column(name = "original_id", nullable = false, length = 50)
    private String originalId;

    @Enumerated(EnumType.STRING)
    @Column(name = "retry_type", nullable = false, length = 50)
    private RetryType retryType;

    @Enumerated(EnumType.STRING)
    @Column(name = "retry_status", nullable = false, length = 30)
    private RetryStatus retryStatus;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries = 3;

    @Column(name = "next_retry_time", nullable = false)
    private LocalDateTime nextRetryTime;

    @Column(name = "retry_delay_seconds", nullable = false)
    private Integer retryDelaySeconds = 60;

    @Column(name = "priority", length = 20)
    private String priority = "NORMAL";

    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;

    @Column(name = "endpoint_url", length = 500)
    private String endpointUrl;

    @ElementCollection
    @CollectionTable(name = "retry_data", joinColumns = @JoinColumn(name = "retry_id"))
    @MapKeyColumn(name = "data_key")
    @Column(name = "data_value", length = 1000)
    private Map<String, String> retryData;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "last_error_code", length = 50)
    private String lastErrorCode;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "last_updated_by", length = 100)
    private String lastUpdatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_retry_time")
    private LocalDateTime lastRetryTime;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Check if retry attempt has exceeded maximum retries
     */
    public boolean isMaxRetriesExceeded() {
        return retryCount >= maxRetries;
    }

    /**
     * Check if retry is ready for execution
     */
    public boolean isReadyForRetry() {
        return retryStatus == RetryStatus.PENDING && 
               LocalDateTime.now().isAfter(nextRetryTime) &&
               !isMaxRetriesExceeded();
    }

    /**
     * Increment retry count and update next retry time
     */
    public void incrementRetryCount() {
        this.retryCount++;
        this.lastRetryTime = LocalDateTime.now();
        
        if (isMaxRetriesExceeded()) {
            this.retryStatus = RetryStatus.MAX_RETRIES_EXCEEDED;
        } else {
            // Exponential backoff: delay = base_delay * 2^(retry_count - 1)
            long backoffSeconds = retryDelaySeconds * (long) Math.pow(2, retryCount - 1);
            this.nextRetryTime = LocalDateTime.now().plusSeconds(backoffSeconds);
        }
    }
}