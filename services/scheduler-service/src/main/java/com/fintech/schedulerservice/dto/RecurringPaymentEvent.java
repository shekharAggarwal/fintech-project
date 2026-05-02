package com.fintech.schedulerservice.dto;

import com.fintech.schedulerservice.entity.PaymentFrequency;
import com.fintech.schedulerservice.entity.RecurringPaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Event DTO for recurring payment events published to Kafka
 */
public class RecurringPaymentEvent {

    private String eventType;
    private String paymentId;
    private String userId;
    private String sourceAccountId;
    private String destinationAccountId;
    private BigDecimal amount;
    private String currency;
    private PaymentFrequency frequency;
    private RecurringPaymentStatus status;
    private LocalDate executionDate;
    private String errorMessage;
    private Instant timestamp;

    public RecurringPaymentEvent() {
        this.timestamp = Instant.now();
    }

    public RecurringPaymentEvent(String eventType, String paymentId, String userId,
                                  String sourceAccountId, String destinationAccountId,
                                  BigDecimal amount, String currency) {
        this.eventType = eventType;
        this.paymentId = paymentId;
        this.userId = userId;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.currency = currency;
        this.timestamp = Instant.now();
    }

    // Getters and Setters
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

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

    public RecurringPaymentStatus getStatus() { return status; }
    public void setStatus(RecurringPaymentStatus status) { this.status = status; }

    public LocalDate getExecutionDate() { return executionDate; }
    public void setExecutionDate(LocalDate executionDate) { this.executionDate = executionDate; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
