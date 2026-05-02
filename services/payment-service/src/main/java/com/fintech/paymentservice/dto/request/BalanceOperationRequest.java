package com.fintech.paymentservice.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record BalanceOperationRequest(
    @NotBlank(message = "Account number is required")
    String accountNumber,

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0001", message = "Amount must be greater than zero")
    BigDecimal amount,

    String currency,

    String description,

    String referenceId
) {}
