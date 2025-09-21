package com.fintech.transactionservice.entity;

import com.fintech.security.annotation.FieldAccessControl;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
public class Transaction {

    @Id
    @Column(name = "txn_id", nullable = false, length = 20)
    @FieldAccessControl(resourceType = "transaction", fieldName = "txnId")
    private String txnId;

    @Column(name = "payment_id", nullable = false, length = 20)
    @FieldAccessControl(resourceType = "transaction", fieldName = "paymentId")
    private String paymentId;


    @Column(name = "user_id", nullable = false, length = 36)
    @FieldAccessControl(resourceType = "transaction", fieldName = "userId")
    private String userId;

    @Column(name = "from_account", nullable = false, length = 50)
    @FieldAccessControl(resourceType = "transaction", fieldName = "fromAccount", sensitive = true)
    private String fromAccount;

    @Column(name = "to_account", nullable = false, length = 50)
    @FieldAccessControl(resourceType = "transaction", fieldName = "toAccount", sensitive = true)
    private String toAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    @FieldAccessControl(resourceType = "transaction", fieldName = "amount")
    private BigDecimal amount;

    @Column(length = 500)
    @FieldAccessControl(resourceType = "transaction", fieldName = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @FieldAccessControl(resourceType = "transaction", fieldName = "status")
    private TransactionStatus status;

    @Column(name = "failure_reason", length = 1000)
    @FieldAccessControl(resourceType = "transaction", fieldName = "failureReason")
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @FieldAccessControl(resourceType = "transaction", fieldName = "createdAt")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @FieldAccessControl(resourceType = "transaction", fieldName = "updatedAt")
    private Instant updatedAt;

    @Column(name = "processing_started_at")
    @FieldAccessControl(resourceType = "transaction", fieldName = "processingStartedAt")
    private Instant processingStartedAt;

    @Column(name = "completed_at")
    @FieldAccessControl(resourceType = "transaction", fieldName = "completedAt")
    private Instant completedAt;

    @Column(name = "failed_at")
    @FieldAccessControl(resourceType = "transaction", fieldName = "failedAt")
    private Instant failedAt;

    @Column(name = "retry_count", nullable = false)
    @FieldAccessControl(resourceType = "transaction", fieldName = "retryCount")
    private int retryCount = 0;


    public Transaction() {
    }

    public Transaction(String txnId, String paymentId, String userId, String fromAccount, String toAccount, BigDecimal amount, String description) {
        this.txnId = txnId;
        this.paymentId = paymentId;
        this.userId = userId;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.description = description;
        this.status = TransactionStatus.PENDING;
    }

    public void setTxnId(String txnId) {
        this.txnId = txnId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setFromAccount(String fromAccount) {
        this.fromAccount = fromAccount;
    }

    public void setToAccount(String toAccount) {
        this.toAccount = toAccount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setProcessingStartedAt(Instant processingStartedAt) {
        this.processingStartedAt = processingStartedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public void setFailedAt(Instant failedAt) {
        this.failedAt = failedAt;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public String getTxnId() {
        return txnId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public String getUserId() {
        return userId;
    }

    public String getFromAccount() {
        return fromAccount;
    }

    public String getToAccount() {
        return toAccount;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getProcessingStartedAt() {
        return processingStartedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public int getRetryCount() {
        return retryCount;
    }
}
