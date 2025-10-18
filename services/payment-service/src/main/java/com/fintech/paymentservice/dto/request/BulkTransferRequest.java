package com.fintech.paymentservice.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BulkTransferRequest(
        @NotEmpty(message = "Transfer list cannot be empty")
        @Size(max = 100, message = "Maximum 100 transfers allowed per bulk request")
        @Valid
        List<InitiateRequest> transfers
) {
}