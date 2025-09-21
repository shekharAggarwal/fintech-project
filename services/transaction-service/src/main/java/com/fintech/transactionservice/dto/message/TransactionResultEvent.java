package com.fintech.transactionservice.dto.message;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event DTO for transaction processing results
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransactionResultEvent {
    
    private String paymentId;
    private String transactionId;
    private String ledgerEntryId;
    private String status;
    private String payerId;
    private String receiverId;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String errorMessage;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime processedAt;

    // Default constructor
    public TransactionResultEvent() {}

    // Constructor for success case
    public TransactionResultEvent(String paymentId, String transactionId, String ledgerEntryId,
                                 String status, String payerId, String receiverId,
                                 BigDecimal amount, String currency) {
        this.paymentId = paymentId;
        this.transactionId = transactionId;
        this.ledgerEntryId = ledgerEntryId;
        this.status = status;
        this.payerId = payerId;
        this.receiverId = receiverId;
        this.amount = amount;
        this.currency = currency;
    }

    // Constructor for error case
    public TransactionResultEvent(String paymentId, String transactionId, String status, String errorMessage) {
        this.paymentId = paymentId;
        this.transactionId = transactionId;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    // Getters and Setters
    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getLedgerEntryId() { return ledgerEntryId; }
    public void setLedgerEntryId(String ledgerEntryId) { this.ledgerEntryId = ledgerEntryId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPayerId() { return payerId; }
    public void setPayerId(String payerId) { this.payerId = payerId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }

    @Override
    public String toString() {
        return "TransactionResultEvent{" +
                "paymentId='" + paymentId + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", status='" + status + '\'' +
                ", amount=" + amount +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}