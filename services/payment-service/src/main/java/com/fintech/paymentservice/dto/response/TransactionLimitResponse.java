package com.fintech.paymentservice.dto.response;

import com.fintech.paymentservice.entity.LimitType;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionLimitResponse(
    Long id,
    String accountId,
    LimitType limitType,
    BigDecimal maxAmount,
    BigDecimal currentUsage,
    BigDecimal remainingAmount,
    Instant resetAt,
    String currency,
    boolean enabled
) {}
