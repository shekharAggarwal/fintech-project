package com.fintech.transactionservice.dto.response;

import com.fintech.transactionservice.entity.DisputeStatus;
import com.fintech.transactionservice.entity.DisputeType;

import java.math.BigDecimal;
import java.time.Instant;

public class DisputeResponse {

    private String id;
    private String transactionId;
    private DisputeType disputeType;
    private DisputeStatus status;
    private String reason;
    private String evidence;
    private BigDecimal refundAmount;
    private String resolution;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant resolvedAt;

    public DisputeResponse() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public DisputeType getDisputeType() { return disputeType; }
    public void setDisputeType(DisputeType disputeType) { this.disputeType = disputeType; }

    public DisputeStatus getStatus() { return status; }
    public void setStatus(DisputeStatus status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }

    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
}
