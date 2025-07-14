package com.fintech.paymentservice.controller;

import com.fintech.paymentservice.service.PaymentService;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@Observed
public class PaymentController {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    
    @Autowired
    private PaymentService paymentService;
    
    @PostMapping("/process")
    public ResponseEntity<PaymentService.PaymentResponse> processPayment(@RequestBody PaymentRequest request) {
        logger.info("Received payment request: {}", request);
        
        try {
            // Validate payment first
            if (!paymentService.validatePayment(request.getPaymentId(), request.getAmount())) {
                logger.warn("Payment validation failed for: {}", request.getPaymentId());
                return ResponseEntity.badRequest().body(
                    new PaymentService.PaymentResponse(
                        request.getPaymentId(),
                        "VALIDATION_FAILED",
                        "Payment validation failed",
                        java.time.LocalDateTime.now()
                    )
                );
            }
            
            PaymentService.PaymentResponse response = paymentService.processPayment(
                request.getPaymentId(),
                request.getAmount(),
                request.getUserId()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error processing payment: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(
                new PaymentService.PaymentResponse(
                    request.getPaymentId(),
                    "ERROR",
                    "Internal server error",
                    java.time.LocalDateTime.now()
                )
            );
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "payment-service"));
    }
    
    // DTO for request
    public static class PaymentRequest {
        private String paymentId;
        private BigDecimal amount;
        private String userId;
        private String description;
        
        // Constructors
        public PaymentRequest() {}
        
        public PaymentRequest(String paymentId, BigDecimal amount, String userId, String description) {
            this.paymentId = paymentId;
            this.amount = amount;
            this.userId = userId;
            this.description = description;
        }
        
        // Getters and Setters
        public String getPaymentId() { return paymentId; }
        public void setPaymentId(String paymentId) { this.paymentId = paymentId; }
        
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        @Override
        public String toString() {
            return String.format("PaymentRequest{paymentId='%s', amount=%s, userId='%s', description='%s'}", 
                paymentId, amount, userId, description);
        }
    }
}
