package com.fintech.schedulerservice.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Entity representing a recurring payment schedule
 */
@Entity
@Table(name = "recurring_payments", indexes = {
    @Index(name = "idx_rp_user_id", columnList = "user_id"),
    @Index(name = "idx_rp_status", columnList = "status"),
    @Index(name = "idx_rp_next_execution", columnList = "next_execution_date"),
    @Index(name = "idx_rp_frequency", columnList = "frequency")
})
public class RecurringPayment {

    @Id
    @Column(name = "id", nullable = false, length = 20)
    private String id;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "source_account_id", nullable = false, length = 50)
    private String sourceAccountId;

    @Column(name = "destination_account_id", nullable = false, length = 50)
    private String destinationAccountId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "frequency", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PaymentFrequency frequency;

    @Column(name = "next_execution_date")
    private LocalDate nextExecutionDate;

    @Column(name = "last_execution_date")
    private LocalDate lastExecutionDate;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RecurringPaymentStatus status = RecurringPaymentStatus.ACTIVE;

    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries = 3;

    @Column(name = "current_retry_count", nullable = false)
    private Integer currentRetryCount = 0;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "quartz_job_key", length = 100)
    private String quartzJobKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructors
    public RecurringPayment() {}

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getSourceAccountId() { return sourceAccountId; }
    public void setSourceAccountId(String sourceAccountId) { this.sourceAccountId = sourceAccountId; }

    public String getDestinationAccountId() { return destinationAccountId; }
    public void setDestinationAccountId(String destinationAccountId) { this.destinationAccountId = destinationAccountId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public PaymentFrequency getFrequency() { return frequency; }
    public void setFrequency(PaymentFrequency frequency) { this.frequency = frequency; }

    public LocalDate getNextExecutionDate() { return nextExecutionDate; }
    public void setNextExecutionDate(LocalDate nextExecutionDate) { this.nextExecutionDate = nextExecutionDate; }

    public LocalDate getLastExecutionDate() { return lastExecutionDate; }
    public void setLastExecutionDate(LocalDate lastExecutionDate) { this.lastExecutionDate = lastExecutionDate; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public RecurringPaymentStatus getStatus() { return status; }
    public void setStatus(RecurringPaymentStatus status) { this.status = status; }

    public Integer getMaxRetries() { return maxRetries; }
    public void setMaxRetries(Integer maxRetries) { this.maxRetries = maxRetries; }

    public Integer getCurrentRetryCount() { return currentRetryCount; }
    public void setCurrentRetryCount(Integer currentRetryCount) { this.currentRetryCount = currentRetryCount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getQuartzJobKey() { return quartzJobKey; }
    public void setQuartzJobKey(String quartzJobKey) { this.quartzJobKey = quartzJobKey; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
