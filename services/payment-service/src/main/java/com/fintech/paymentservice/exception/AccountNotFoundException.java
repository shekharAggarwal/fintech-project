package com.fintech.paymentservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class AccountNotFoundException extends RuntimeException {

    private final String accountIdentifier;

    public AccountNotFoundException(String accountIdentifier) {
        super("Account not found: " + accountIdentifier);
        this.accountIdentifier = accountIdentifier;
    }

    public AccountNotFoundException(String accountIdentifier, String message) {
        super(message);
        this.accountIdentifier = accountIdentifier;
    }

    public String getAccountIdentifier() { return accountIdentifier; }
}
