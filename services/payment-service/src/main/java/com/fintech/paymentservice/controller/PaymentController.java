package com.fintech.paymentservice.controller;

import com.fintech.paymentservice.dto.request.BulkTransferRequest;
import com.fintech.paymentservice.dto.request.DepositRequest;
import com.fintech.paymentservice.dto.request.InitiateRequest;
import com.fintech.paymentservice.dto.request.WithdrawRequest;
import com.fintech.paymentservice.dto.response.BulkTransferResponse;
import com.fintech.paymentservice.dto.response.PaymentHistoryResponse;
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
     * Deposit money to account
     */
    @PostMapping("/deposit")
    @RequireAuthorization(message = "Access denied: Authentication required for deposit", resourceType = "payment")
    @FilterResponse(resourceType = "payment")
    public ResponseEntity<?> deposit(@Valid @RequestBody DepositRequest request) {
        String currentUserId = authorizationService.getCurrentUserId();
        logger.info("User {} initiating deposit to {} for amount {}", 
                    currentUserId, request.account(), request.amount());

        if (currentUserId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No user context", "reason", "Unable to determine current user"));
        }

        try {
            PaymentInitiatedResponse response = paymentService.deposit(
                request.account(), request.amount(), request.description(), currentUserId);
            logger.info("Deposit initiated successfully: {}", response.paymentId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to initiate deposit for user {}: {}", currentUserId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Deposit initiation failed", "reason", e.getMessage()));
        }
    }

    /**
     * Withdraw money from account
     */
    @PostMapping("/withdraw")
    @RequireAuthorization(message = "Access denied: Authentication required for withdrawal", resourceType = "payment")
    @FilterResponse(resourceType = "payment")
    public ResponseEntity<?> withdraw(@Valid @RequestBody WithdrawRequest request) {
        String currentUserId = authorizationService.getCurrentUserId();
        logger.info("User {} initiating withdrawal from {} for amount {}", 
                    currentUserId, request.account(), request.amount());

        if (currentUserId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No user context", "reason", "Unable to determine current user"));
        }

        try {
            PaymentInitiatedResponse response = paymentService.withdraw(
                request.account(), request.amount(), request.description(), currentUserId);
            logger.info("Withdrawal initiated successfully: {}", response.paymentId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to initiate withdrawal for user {}: {}", currentUserId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Withdrawal initiation failed", "reason", e.getMessage()));
        }
    }

    /**
     * Get payment history
     */
    @GetMapping("/history")
    @RequireAuthorization(message = "Access denied: Authentication required to view payment history", resourceType = "payment")
    @FilterResponse(resourceType = "payment")
    public ResponseEntity<?> getPaymentHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        String currentUserId = authorizationService.getCurrentUserId();
        logger.info("User {} requesting payment history - page: {}, size: {}", currentUserId, page, size);

        if (currentUserId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No user context", "reason", "Unable to determine current user"));
        }

        try {
            org.springframework.data.domain.Page<Payment> paymentPage = paymentService.getPaymentHistory(currentUserId, page, size);
            
            PaymentHistoryResponse response = new PaymentHistoryResponse(
                paymentPage.getContent(),
                (int) paymentPage.getTotalElements(),
                paymentPage.getNumber(),
                paymentPage.getSize(),
                paymentPage.hasNext()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get payment history for user {}: {}", currentUserId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to get payment history", "reason", e.getMessage()));
        }
    }

    /**
     * Cancel pending payment
     */
    @PostMapping("/{paymentId}/cancel")
    @RequireAuthorization(message = "Access denied: Authentication required to cancel payment", resourceType = "payment")
    public ResponseEntity<?> cancelPayment(@PathVariable String paymentId) {
        String currentUserId = authorizationService.getCurrentUserId();
        logger.info("User {} cancelling payment: {}", currentUserId, paymentId);

        if (currentUserId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No user context", "reason", "Unable to determine current user"));
        }

        try {
            boolean cancelled = paymentService.cancelPayment(paymentId, currentUserId);

            if (cancelled) {
                logger.info("Payment cancelled successfully: {}", paymentId);
                return ResponseEntity.ok(Map.of(
                        "message", "Payment cancelled successfully",
                        "paymentId", paymentId,
                        "status", "CANCELLED"
                ));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Cannot cancel payment", "reason", "Payment is not in a cancellable state or not found"));
            }
        } catch (Exception e) {
            logger.error("Failed to cancel payment {}: {}", paymentId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Payment cancellation failed", "reason", e.getMessage()));
        }
    }

    /**
     * Bulk transfer operations
     */
    @PostMapping("/bulk-transfer")
    @RequireAuthorization(message = "Access denied: Authentication required for bulk transfer", resourceType = "payment")
    @FilterResponse(resourceType = "payment")
    public ResponseEntity<?> bulkTransfer(@Valid @RequestBody BulkTransferRequest request) {
        String currentUserId = authorizationService.getCurrentUserId();
        logger.info("User {} initiating bulk transfer with {} requests", 
                    currentUserId, request.transfers().size());

        if (currentUserId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No user context", "reason", "Unable to determine current user"));
        }

        try {
            BulkTransferResponse response = paymentService.processBulkTransfers(request.transfers(), currentUserId);
            logger.info("Bulk transfer completed for user {}: {} successful, {} failed", 
                       currentUserId, response.successful(), response.failed());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to process bulk transfer for user {}: {}", currentUserId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Bulk transfer failed", "reason", e.getMessage()));
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