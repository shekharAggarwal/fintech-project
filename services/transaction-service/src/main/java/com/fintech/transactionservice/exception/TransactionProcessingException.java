package com.fintech.transactionservice.exception;

public class TransactionProcessingException extends RuntimeException {

    private final String transactionId;
    private final boolean retryable;

    public TransactionProcessingException(String message, String transactionId) {
        super(message);
        this.transactionId = transactionId;
        this.retryable = false;
    }

    public TransactionProcessingException(String message, String transactionId, boolean retryable) {
        super(message);
        this.transactionId = transactionId;
        this.retryable = retryable;
    }

    public TransactionProcessingException(String message, String transactionId, Throwable cause) {
        super(message, cause);
        this.transactionId = transactionId;
        this.retryable = false;
    }

    public TransactionProcessingException(String message, String transactionId, boolean retryable, Throwable cause) {
        super(message, cause);
        this.transactionId = transactionId;
        this.retryable = retryable;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public boolean isRetryable() {
        return retryable;
    }
}
