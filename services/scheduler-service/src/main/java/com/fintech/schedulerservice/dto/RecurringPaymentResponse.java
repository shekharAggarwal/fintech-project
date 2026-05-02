package com.fintech.schedulerservice.dto;

import com.fintech.schedulerservice.entity.PaymentFrequency;
import com.fintech.schedulerservice.entity.RecurringPaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Response DTO for recurring payment data
 */
public class RecurringPaymentResponse {

    private String id;
    private String userId;
    private String sourceAccountId;
    private String destinationAccountId;
    private BigDecimal amount;
    private String currency;
    private PaymentFrequency frequency;
    private LocalDate nextExecutionDate;
    private LocalDate lastExecutionDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private RecurringPaymentStatus status;
    private Integer maxRetries;
    private Integer currentRetryCount;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;

    public RecurringPaymentResponse() {}

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

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
