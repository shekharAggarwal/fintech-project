package com.fintech.paymentservice.entity;

import com.fintech.paymentservice.model.PaymentStatus;
import com.fintech.security.annotation.FieldAccessControl;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_user_id", columnList = "user_id"),
    @Index(name = "idx_payment_status", columnList = "status"),
    @Index(name = "idx_payment_created_at", columnList = "created_at"),
    @Index(name = "idx_payment_from_account", columnList = "from_account"),
    @Index(name = "idx_payment_to_account", columnList = "to_account")
})
public class Payment {

    @Id
    @Column(name = "payment_id", nullable = false, length = 20)
    @FieldAccessControl(resourceType = "payment", fieldName = "paymentId")
    private String paymentId;

    @Column(name = "user_id", nullable = false, length = 36)
    @FieldAccessControl(resourceType = "payment", fieldName = "userId")
    private String userId;

    @Column(name = "from_account", nullable = false, length = 50)
    @FieldAccessControl(resourceType = "payment", fieldName = "fromAccount", sensitive = true)
    private String fromAccount;

    @Column(name = "to_account", nullable = false, length = 50)
    @FieldAccessControl(resourceType = "payment", fieldName = "toAccount", sensitive = true)
    private String toAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    @FieldAccessControl(resourceType = "payment", fieldName = "amount")
    private BigDecimal amount;

    @Column(length = 500)
    @FieldAccessControl(resourceType = "payment", fieldName = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @FieldAccessControl(resourceType = "payment", fieldName = "status")
    private PaymentStatus status;

    @Column(name = "failure_reason", length = 1000)
    @FieldAccessControl(resourceType = "payment", fieldName = "failureReason")
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @FieldAccessControl(resourceType = "payment", fieldName = "createdAt")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @FieldAccessControl(resourceType = "payment", fieldName = "updatedAt")
    private Instant updatedAt;

    @Column(name = "authorized_at")
    @FieldAccessControl(resourceType = "payment", fieldName = "authorizedAt")
    private Instant authorizedAt;

    @Column(name = "processing_started_at")
    @FieldAccessControl(resourceType = "payment", fieldName = "processingStartedAt")
    private Instant processingStartedAt;

    @Column(name = "completed_at")
    @FieldAccessControl(resourceType = "payment", fieldName = "completedAt")
    private Instant completedAt;

    @Column(name = "failed_at")
    @FieldAccessControl(resourceType = "payment", fieldName = "failedAt")
    private Instant failedAt;

    @Column(name = "retry_count", nullable = false)
    @FieldAccessControl(resourceType = "payment", fieldName = "retryCount")
    private int retryCount = 0;

    // Constructors
    public Payment() {
        this.status = PaymentStatus.PENDING;
        this.retryCount = 0;
    }

    public Payment(String paymentId, String userId, String fromAccount, String toAccount, BigDecimal amount) {
        this.paymentId = paymentId;
        this.userId = userId;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.status = PaymentStatus.PENDING;
        this.retryCount = 0;
    }

    // Getters and Setters
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

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getAuthorizedAt() {
        return authorizedAt;
    }

    public void setAuthorizedAt(Instant authorizedAt) {
        this.authorizedAt = authorizedAt;
    }

    public Instant getProcessingStartedAt() {
        return processingStartedAt;
    }

    public void setProcessingStartedAt(Instant processingStartedAt) {
        this.processingStartedAt = processingStartedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(Instant failedAt) {
        this.failedAt = failedAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    @Override
    public String toString() {
        return "Payment{" +
                "paymentId='" + paymentId + '\'' +
                ", userId='" + userId + '\'' +
                ", fromAccount='" + fromAccount + '\'' +
                ", toAccount='" + toAccount + '\'' +
                ", amount=" + amount +
                ", status=" + status +
                ", retryCount=" + retryCount +
                ", createdAt=" + createdAt +
                '}';
    }
}