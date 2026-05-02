package com.fintech.transactionservice.exception;

public class TransactionNotFoundException extends RuntimeException {

    private final String transactionId;

    public TransactionNotFoundException(String transactionId) {
        super("Transaction not found with id: " + transactionId);
        this.transactionId = transactionId;
    }

    public TransactionNotFoundException(String message, String transactionId) {
        super(message);
        this.transactionId = transactionId;
    }

    public String getTransactionId() {
        return transactionId;
    }
}
