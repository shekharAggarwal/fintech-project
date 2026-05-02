package com.fintech.transactionservice.dto.request;

import com.fintech.transactionservice.entity.DisputeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class DisputeRequest {

    @NotBlank(message = "Transaction ID is required")
    private String transactionId;

    @NotNull(message = "Dispute type is required")
    private DisputeType disputeType;

    @NotBlank(message = "Reason is required")
    private String reason;

    private String evidence; // JSON string

    private BigDecimal refundAmount;

    public DisputeRequest() {}

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }

    public DisputeType getDisputeType() { return disputeType; }
    public void setDisputeType(DisputeType disputeType) { this.disputeType = disputeType; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }

    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
}
