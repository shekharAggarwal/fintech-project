package com.fintech.transactionservice.model;

public record TransactionResult(boolean success, String statusCode, String transactionId) {
}
