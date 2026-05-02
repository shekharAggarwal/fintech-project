package com.fintech.transactionservice.dto.response;

import com.fintech.transactionservice.entity.ReversalStatus;
import com.fintech.transactionservice.entity.ReversalType;

import java.math.BigDecimal;
import java.time.Instant;

public class ReversalResponse {

    private String id;
    private String originalTransactionId;
    private String reversalTransactionId;
    private ReversalType reversalType;
    private BigDecimal amount;
    private ReversalStatus status;
    private String reason;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;

    public ReversalResponse() {}

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
