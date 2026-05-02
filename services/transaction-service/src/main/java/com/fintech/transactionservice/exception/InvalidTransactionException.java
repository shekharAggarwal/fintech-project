package com.fintech.transactionservice.exception;

public class InvalidTransactionException extends RuntimeException {

    private final String field;

    public InvalidTransactionException(String message) {
        super(message);
        this.field = null;
    }

    public InvalidTransactionException(String message, String field) {
        super(message);
        this.field = field;
    }

    public InvalidTransactionException(String message, Throwable cause) {
        super(message, cause);
        this.field = null;
    }

    public String getField() {
        return field;
    }
}
