package com.fintech.transactionservice.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
@Observed
public class TransactionService {
    
    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);
    private final Random random = new Random();
    
    @CircuitBreaker(name = "paymentService", fallbackMethod = "fallbackCreateTransaction")
    @Retry(name = "transactionProcessing")
    public TransactionResponse createTransaction(TransactionRequest request) {
        logger.info("Creating transaction: {} for amount: {}", request.getTransactionId(), request.getAmount());
        
        // Simulate processing time
        try {
            Thread.sleep(random.nextInt(800) + 300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Simulate failure for demonstration
        if (random.nextDouble() < 0.25) {
            logger.error("Transaction creation failed for: {}", request.getTransactionId());
            throw new TransactionProcessingException("Transaction processing failed");
        }
        
        TransactionResponse response = new TransactionResponse(
            request.getTransactionId(),
            "COMPLETED",
            "Transaction created successfully",
            LocalDateTime.now(),
            request.getAmount()
        );
        
        logger.info("Transaction created successfully: {}", response);
        return response;
    }
    
    @CircuitBreaker(name = "dbAccess", fallbackMethod = "fallbackGetTransaction")
    public TransactionResponse getTransaction(String transactionId) {
        logger.debug("Retrieving transaction: {}", transactionId);
        
        // Simulate database access
        try {
            Thread.sleep(random.nextInt(200) + 100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Simulate occasional database failure
        if (random.nextDouble() < 0.15) {
            throw new DatabaseAccessException("Database temporarily unavailable");
        }
        
        return new TransactionResponse(
            transactionId,
            "COMPLETED",
            "Transaction retrieved",
            LocalDateTime.now().minusMinutes(random.nextInt(60)),
            new BigDecimal("100.00")
        );
    }
    
    // Fallback methods
    public TransactionResponse fallbackCreateTransaction(TransactionRequest request, Exception ex) {
        logger.warn("Circuit breaker activated for transaction creation: {}. Reason: {}", 
                   request.getTransactionId(), ex.getMessage());
        
        return new TransactionResponse(
            request.getTransactionId(),
            "FAILED",
            "Transaction service temporarily unavailable",
            LocalDateTime.now(),
            request.getAmount()
        );
    }
    
    public TransactionResponse fallbackGetTransaction(String transactionId, Exception ex) {
        logger.warn("Circuit breaker activated for transaction retrieval: {}. Reason: {}", 
                   transactionId, ex.getMessage());
        
        return new TransactionResponse(
            transactionId,
            "UNKNOWN",
            "Transaction data temporarily unavailable",
            LocalDateTime.now(),
            BigDecimal.ZERO
        );
    }
    
    // DTO Classes
    public static class TransactionRequest {
        private String transactionId;
        private BigDecimal amount;
        private String fromAccount;
        private String toAccount;
        private String description;
        
        public TransactionRequest() {
            this.transactionId = UUID.randomUUID().toString();
        }
        
        public TransactionRequest(BigDecimal amount, String fromAccount, String toAccount, String description) {
            this();
            this.amount = amount;
            this.fromAccount = fromAccount;
            this.toAccount = toAccount;
            this.description = description;
        }
        
        // Getters and Setters
        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
        
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        
        public String getFromAccount() { return fromAccount; }
        public void setFromAccount(String fromAccount) { this.fromAccount = fromAccount; }
        
        public String getToAccount() { return toAccount; }
        public void setToAccount(String toAccount) { this.toAccount = toAccount; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    public static class TransactionResponse {
        private final String transactionId;
        private final String status;
        private final String message;
        private final LocalDateTime timestamp;
        private final BigDecimal amount;
        
        public TransactionResponse(String transactionId, String status, String message, 
                                 LocalDateTime timestamp, BigDecimal amount) {
            this.transactionId = transactionId;
            this.status = status;
            this.message = message;
            this.timestamp = timestamp;
            this.amount = amount;
        }
        
        // Getters
        public String getTransactionId() { return transactionId; }
        public String getStatus() { return status; }
        public String getMessage() { return message; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public BigDecimal getAmount() { return amount; }
        
        @Override
        public String toString() {
            return String.format("TransactionResponse{id='%s', status='%s', amount=%s, timestamp=%s}", 
                transactionId, status, amount, timestamp);
        }
    }
    
    public static class TransactionProcessingException extends RuntimeException {
        public TransactionProcessingException(String message) {
            super(message);
        }
    }
    
    public static class DatabaseAccessException extends RuntimeException {
        public DatabaseAccessException(String message) {
            super(message);
        }
    }
}
