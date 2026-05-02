package com.fintech.paymentservice.dto.request;

import com.fintech.paymentservice.entity.LimitType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransactionLimitRequest(
    @NotNull(message = "Limit type is required")
    LimitType limitType,

    @NotNull(message = "Max amount is required")
    @DecimalMin(value = "0.01", message = "Max amount must be greater than zero")
    BigDecimal maxAmount,

    Boolean enabled
) {}
