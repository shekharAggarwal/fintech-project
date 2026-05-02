package com.fintech.transactionservice.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "reversals")
public class Reversal {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Column(name = "original_transaction_id", nullable = false, length = 20)
    private String originalTransactionId;

    @Column(name = "reversal_transaction_id", length = 20)
    private String reversalTransactionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reversal_type", nullable = false)
    private ReversalType reversalType;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReversalStatus status;

    @Column(name = "reason", length = 1000)
    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public Reversal() {}

    public Reversal(String id, String originalTransactionId, ReversalType reversalType, BigDecimal amount, String reason) {
        this.id = id;
        this.originalTransactionId = originalTransactionId;
        this.reversalType = reversalType;
        this.amount = amount;
        this.reason = reason;
        this.status = ReversalStatus.INITIATED;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOriginalTransactionId() { return originalTransactionId; }
    public void setOriginalTransactionId(String originalTransactionId) { this.originalTransactionId = originalTransactionId; }

    public String getReversalTransactionId() { return reversalTransactionId; }
    public void setReversalTransactionId(String reversalTransactionId) { this.reversalTransactionId = reversalTransactionId; }

    public ReversalType getReversalType() { return reversalType; }
    public void setReversalType(ReversalType reversalType) { this.reversalType = reversalType; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public ReversalStatus getStatus() { return status; }
    public void setStatus(ReversalStatus status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
