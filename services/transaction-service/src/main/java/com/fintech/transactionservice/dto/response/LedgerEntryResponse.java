package com.fintech.transactionservice.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO from ledger service
 */
public class LedgerEntryResponse {
    
    private String status;
    private String transactionId;
    private String txnId;
    private String debitAccountId;
    private String creditAccountId;
    private BigDecimal amount;
    private String currency;
    private String description;
    private LocalDateTime createdAt;
    private String errorMessage;

    // Default constructor
    public LedgerEntryResponse() {}

    // Constructor with basic fields
    public LedgerEntryResponse(String status, String transactionId, String errorMessage) {
        this.status = status;
        this.transactionId = transactionId;
        this.errorMessage = errorMessage;
    }

    // Getters and Setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public String getTxnId() { return txnId; }
    public void setTxnId(String txnId) { this.txnId = txnId; }

    public String getDebitAccountId() { return debitAccountId; }
    public void setDebitAccountId(String debitAccountId) { this.debitAccountId = debitAccountId; }

    public String getCreditAccountId() { return creditAccountId; }
    public void setCreditAccountId(String creditAccountId) { this.creditAccountId = creditAccountId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(status);
    }

    @Override
    public String toString() {
        return "LedgerEntryResponse{" +
                "status='" + status + '\'' +
                ", transactionId='" + transactionId + '\'' +
                ", txnId='" + txnId + '\'' +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}