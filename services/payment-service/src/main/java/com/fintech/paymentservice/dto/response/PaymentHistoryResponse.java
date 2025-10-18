package com.fintech.paymentservice.dto.response;

import com.fintech.paymentservice.entity.Payment;

import java.util.List;

public record PaymentHistoryResponse(
        List<Payment> payments,
        int totalCount,
        int page,
        int size,
        boolean hasNext
) {
}