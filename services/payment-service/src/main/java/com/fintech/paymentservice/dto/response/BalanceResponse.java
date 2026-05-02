package com.fintech.paymentservice.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record BalanceResponse(
    Long accountId,
    String accountNumber,
    BigDecimal currentBalance,
    BigDecimal availableBalance,
    BigDecimal holdAmount,
    String currency,
    Instant lastBalanceUpdate
) {}
