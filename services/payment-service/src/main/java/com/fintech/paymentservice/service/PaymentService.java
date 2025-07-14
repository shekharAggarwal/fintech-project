package com.fintech.paymentservice.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.observation.annotation.Observed;
import io.micrometer.tracing.annotation.NewSpan;
import io.micrometer.tracing.annotation.SpanTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;

@Service
@Observed
public class PaymentService {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private final Random random = new Random();
    
    @CircuitBreaker(name = "payment-service", fallbackMethod = "fallbackProcessPayment")
    @Retry(name = "payment-service")
    @NewSpan("payment-processing")
    public PaymentResponse processPayment(@SpanTag("paymentId") String paymentId, 
                                        @SpanTag("amount") BigDecimal amount,
                                        @SpanTag("userId") String userId) {
        logger.info("Processing payment: {} for user: {} with amount: {}", paymentId, userId, amount);
        
        // Simulate processing time
        try {
            Thread.sleep(random.nextInt(1000) + 500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Simulate failure for demonstration
        if (random.nextDouble() < 0.3) {
            logger.error("Payment processing failed for payment: {}", paymentId);
            throw new PaymentProcessingException("Payment processing failed");
        }
        
        PaymentResponse response = new PaymentResponse(
            paymentId,
            "SUCCESS",
            "Payment processed successfully",
            LocalDateTime.now()
        );
        
        logger.info("Payment processed successfully: {}", response);
        return response;
    }
    
    @NewSpan("payment-validation")
    public boolean validatePayment(@SpanTag("paymentId") String paymentId, 
                                 @SpanTag("amount") BigDecimal amount) {
        logger.debug("Validating payment: {} with amount: {}", paymentId, amount);
        
        // Simulate validation logic
        boolean isValid = amount.compareTo(BigDecimal.ZERO) > 0 && 
                         amount.compareTo(new BigDecimal("10000")) <= 0;
        
        logger.debug("Payment validation result for {}: {}", paymentId, isValid);
        return isValid;
    }
    
    // Fallback method for circuit breaker
    public PaymentResponse fallbackProcessPayment(String paymentId, BigDecimal amount, String userId, Exception ex) {
        logger.warn("Circuit breaker activated for payment: {}. Reason: {}", paymentId, ex.getMessage());
        
        return new PaymentResponse(
            paymentId,
            "FAILED",
            "Payment service temporarily unavailable. Please try again later.",
            LocalDateTime.now()
        );
    }
    
    // DTO Classes
    public static class PaymentResponse {
        private final String paymentId;
        private final String status;
        private final String message;
        private final LocalDateTime timestamp;
        
        public PaymentResponse(String paymentId, String status, String message, LocalDateTime timestamp) {
            this.paymentId = paymentId;
            this.status = status;
            this.message = message;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getPaymentId() { return paymentId; }
        public String getStatus() { return status; }
        public String getMessage() { return message; }
        public LocalDateTime getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            return String.format("PaymentResponse{paymentId='%s', status='%s', message='%s', timestamp=%s}", 
                paymentId, status, message, timestamp);
        }
    }
    
    public static class PaymentProcessingException extends RuntimeException {
        public PaymentProcessingException(String message) {
            super(message);
        }
    }
}
