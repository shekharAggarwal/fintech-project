package com.fintech.paymentservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class InsufficientFundsException extends RuntimeException {

    private final String accountNumber;
    private final String currency;

    public InsufficientFundsException(String message) {
        super(message);
        this.accountNumber = null;
        this.currency = null;
    }

    public InsufficientFundsException(String accountNumber, String message) {
        super(message);
        this.accountNumber = accountNumber;
        this.currency = null;
    }

    public InsufficientFundsException(String accountNumber, String currency, String message) {
        super(message);
        this.accountNumber = accountNumber;
        this.currency = currency;
    }

    public String getAccountNumber() { return accountNumber; }
    public String getCurrency() { return currency; }
}
