package com.fintech.transactionservice.dto.request;

import com.fintech.transactionservice.entity.ReversalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public class ReversalRequest {

    @NotBlank(message = "Original transaction ID is required")
    private String originalTransactionId;

    @NotNull(message = "Reversal type is required")
    private ReversalType reversalType;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private String reason;

    public ReversalRequest() {}

    public String getOriginalTransactionId() { return originalTransactionId; }
    public void setOriginalTransactionId(String originalTransactionId) { this.originalTransactionId = originalTransactionId; }

    public ReversalType getReversalType() { return reversalType; }
    public void setReversalType(ReversalType reversalType) { this.reversalType = reversalType; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
