package com.fintech.paymentservice.saga;

public enum SagaStep {
    VALIDATE_FUNDS,
    HOLD_FUNDS,
    INITIATE_TRANSACTION,
    PROCESS,
    DEBIT,
    CREDIT,
    COMPLETE
}
