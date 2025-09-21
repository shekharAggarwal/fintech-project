package com.fintech.paymentservice.dto.message;

import java.math.BigDecimal;

/**
 * Request DTO for creating ledger entries
 */
public class TransactionCompletedMessage {

    private String txnId;
    private String paymentId;
    private String userId;
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private String description;
    private String status;

    // Default constructor
    public TransactionCompletedMessage() {
    }

    public TransactionCompletedMessage(String txnId, String paymentId, String userId, String fromAccount, String toAccount, BigDecimal amount, String description, String status) {
        this.txnId = txnId;
        this.paymentId = paymentId;
        this.userId = userId;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.description = description;
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTxnId() {
        return txnId;
    }

    public void setTxnId(String txnId) {
        this.txnId = txnId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFromAccount() {
        return fromAccount;
    }

    public void setFromAccount(String fromAccount) {
        this.fromAccount = fromAccount;
    }

    public String getToAccount() {
        return toAccount;
    }

    public void setToAccount(String toAccount) {
        this.toAccount = toAccount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}