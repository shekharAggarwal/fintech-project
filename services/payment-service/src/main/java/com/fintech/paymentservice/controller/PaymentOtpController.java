package com.fintech.paymentservice.controller;

import com.fintech.paymentservice.dto.request.OtpVerificationRequest;
import com.fintech.paymentservice.service.OtpService;
import com.fintech.security.annotation.RequireAuthorization;
import com.fintech.security.service.AuthorizationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentOtpController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentOtpController.class);

    private final OtpService otpService;
    private final AuthorizationService authorizationService;

    public PaymentOtpController(OtpService otpService, AuthorizationService authorizationService) {
        this.otpService = otpService;
        this.authorizationService = authorizationService;
    }

    /**
     * Verify OTP for payment authorization
     */
    @PostMapping("/{paymentId}/verify-otp")
    @RequireAuthorization(message = "Access denied: Authentication required for OTP verification", resourceType = "payment")
    public ResponseEntity<?> verifyOtp(
            @PathVariable String paymentId,
            @Valid @RequestBody OtpVerificationRequest otpRequest) {

        String currentUserId = authorizationService.getCurrentUserId();
        logger.info("User {} verifying OTP for payment: {}", currentUserId, paymentId);

        if (currentUserId == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No user context", "reason", "Unable to determine current user"));
        }

        try {
            boolean verified = otpService.verifyOtp(paymentId, otpRequest.otp(), currentUserId);

            if (verified) {
                logger.info("OTP verified successfully for payment: {}", paymentId);
                return ResponseEntity.ok(Map.of(
                        "message", "OTP verified successfully",
                        "paymentId", paymentId,
                        "status", "AUTHORIZED"
                ));
            } else {
                logger.warn("Invalid OTP provided for payment: {}", paymentId);
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid OTP", "reason", "The provided OTP is incorrect or expired"));
            }
        } catch (Exception e) {
            logger.error("Failed to verify OTP for payment {}: {}", paymentId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "OTP verification failed", "reason", e.getMessage()));
        }
    }

}
