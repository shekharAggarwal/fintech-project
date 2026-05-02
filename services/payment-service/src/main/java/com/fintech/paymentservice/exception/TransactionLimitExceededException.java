package com.fintech.paymentservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class TransactionLimitExceededException extends RuntimeException {

    private final String limitType;
    private final String accountId;

    public TransactionLimitExceededException(String message) {
        super(message);
        this.limitType = null;
        this.accountId = null;
    }

    public TransactionLimitExceededException(String limitType, String accountId, String message) {
        super(message);
        this.limitType = limitType;
        this.accountId = accountId;
    }

    public String getLimitType() { return limitType; }
    public String getAccountId() { return accountId; }
}
