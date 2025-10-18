package com.fintech.paymentservice.dto.response;

import java.util.List;

public record BulkTransferResponse(
        List<PaymentInitiatedResponse> successfulTransfers,
        List<BulkTransferError> failedTransfers,
        int totalRequested,
        int successful,
        int failed
) {
    public record BulkTransferError(
            int index,
            String error,
            String reason
    ) {}
}