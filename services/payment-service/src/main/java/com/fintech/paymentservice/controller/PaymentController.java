package com.fintech.paymentservice.controller;

import com.fintech.paymentservice.dto.request.InitiateRequest;
import com.fintech.paymentservice.dto.response.PaymentInitiatedResponse;
import com.fintech.paymentservice.entity.Payment;
import com.fintech.paymentservice.service.PaymentService;
import com.fintech.security.annotation.FilterResponse;
import com.fintech.security.annotation.RequireAuthorization;
import com.fintech.security.service.AuthorizationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;
    private final AuthorizationService authorizationService;

    public PaymentController(PaymentService paymentService, AuthorizationService authorizationService) {
        this.paymentService = paymentService;
        this.authorizationService = authorizationService;
    }

    /**
     * Initiate a new payment
     */
    @PostMapping("/transfer")
    @RequireAuthorization(message = "Access denied: Authentication required for payment initiation", resourceType = "payment")
    @FilterResponse(resourceType = "payment")
    public ResponseEntity<?> initiate(@Valid @RequestBody InitiateRequest request) {
        String currentUserId = authorizationService.getCurrentUserId();
        logger.info("User {} initiating payment from {} to {} for amount {}",
                currentUserId, request.fromAccount(), request.toAccount(), request.amount());

        if (currentUserId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No user context", "reason", "Unable to determine current user"));
        }

        try {
            PaymentInitiatedResponse response = paymentService.initiate(request, currentUserId);
            logger.info("Payment initiated successfully: {}", response.paymentId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to initiate payment for user {}: {}", currentUserId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Payment initiation failed", "reason", e.getMessage()));
        }
    }


    /**
     * Get payment status
     */
    @GetMapping("/{paymentId}")
    @RequireAuthorization(message = "Access denied: Authentication required to view payment", resourceType = "payment")
    @FilterResponse(resourceType = "payment")
    public ResponseEntity<?> getPaymentStatus(@PathVariable String paymentId) {
        String currentUserId = authorizationService.getCurrentUserId();
        logger.info("User {} requesting payment status: {}", currentUserId, paymentId);

        if (currentUserId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No user context", "reason", "Unable to determine current user"));
        }

        try {
            Optional<Payment> payment = paymentService.getPaymentStatus(paymentId, currentUserId);

            if (payment.isPresent()) {
                return ResponseEntity.ok(payment.get());
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Failed to get payment status for {}: {}", paymentId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get payment status", "reason", e.getMessage()));
        }
    }

    /**
     * Retry a stuck payment
     */
   /* @PostMapping("/{paymentId}/retry")
    @RequireAuthorization(message = "Access denied: Authentication required for payment retry", resourceType = "payment")
    public ResponseEntity<?> retry(@PathVariable String paymentId) {
        String currentUserId = authorizationService.getCurrentUserId();
        logger.info("User {} retrying payment: {}", currentUserId, paymentId);

        if (currentUserId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No user context", "reason", "Unable to determine current user"));
        }

        try {
            boolean retryInitiated = paymentService.retry(paymentId, currentUserId);

            if (retryInitiated) {
                logger.info("Payment retry initiated for: {}", paymentId);
                return ResponseEntity.ok(Map.of(
                        "message", "Payment retry initiated",
                        "paymentId", paymentId
                ));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Cannot retry payment", "reason", "Payment is not in a retryable state"));
            }
        } catch (Exception e) {
            logger.error("Failed to retry payment {}: {}", paymentId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Payment retry failed", "reason", e.getMessage()));
        }
    }*/
}