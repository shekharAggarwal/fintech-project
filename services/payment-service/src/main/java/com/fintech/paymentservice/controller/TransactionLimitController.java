package com.fintech.paymentservice.controller;

import com.fintech.paymentservice.dto.request.LimitCheckRequest;
import com.fintech.paymentservice.dto.request.TransactionLimitRequest;
import com.fintech.paymentservice.dto.response.TransactionLimitResponse;
import com.fintech.paymentservice.entity.LimitType;
import com.fintech.paymentservice.exception.TransactionLimitExceededException;
import com.fintech.paymentservice.service.TransactionLimitService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class TransactionLimitController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionLimitController.class);

    private final TransactionLimitService limitService;

    public TransactionLimitController(TransactionLimitService limitService) {
        this.limitService = limitService;
    }

    /**
     * Create/set a transaction limit for an account
     */
    @PostMapping("/{accountId}/limits")
    public ResponseEntity<TransactionLimitResponse> createLimit(
            @PathVariable String accountId,
            @Valid @RequestBody TransactionLimitRequest request) {
        logger.info("Creating/setting {} limit for account {}: maxAmount={}", request.limitType(), accountId, request.maxAmount());
        TransactionLimitResponse response = limitService.updateLimit(
                accountId, request.limitType(), request.maxAmount(), request.enabled());
        return ResponseEntity.status(201).body(response);
    }

    /**
     * Get all limits for an account
     */
    @GetMapping("/{accountId}/limits")
    public ResponseEntity<List<TransactionLimitResponse>> getLimits(@PathVariable String accountId) {
        logger.info("Getting transaction limits for account: {}", accountId);
        List<TransactionLimitResponse> limits = limitService.getLimits(accountId);
        return ResponseEntity.ok(limits);
    }

    /**
     * Update a specific limit type for an account
     */
    @PutMapping("/{accountId}/limits/{type}")
    public ResponseEntity<?> updateLimit(
            @PathVariable String accountId,
            @PathVariable String type,
            @Valid @RequestBody TransactionLimitRequest request) {
        logger.info("Updating {} limit for account {}: maxAmount={}", type, accountId, request.maxAmount());
        try {
            LimitType limitType = LimitType.valueOf(type.toUpperCase());
            TransactionLimitResponse response = limitService.updateLimit(accountId, limitType, request.maxAmount(), request.enabled());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid limit type", "validTypes", List.of(LimitType.values())));
        }
    }

    /**
     * Check if a transaction amount is within limits
     */
    @PostMapping("/{accountId}/limits/check")
    public ResponseEntity<?> checkLimits(
            @PathVariable String accountId,
            @Valid @RequestBody LimitCheckRequest request) {
        logger.info("Checking limits for account {} amount {}", accountId, request.amount());
        try {
            limitService.checkLimits(accountId, request.amount());
            return ResponseEntity.ok(Map.of(
                "allowed", true,
                "accountId", accountId,
                "amount", request.amount()
            ));
        } catch (TransactionLimitExceededException e) {
            return ResponseEntity.status(429)
                .body(Map.of(
                    "allowed", false,
                    "accountId", accountId,
                    "amount", request.amount(),
                    "error", e.getMessage()
                ));
        }
    }
}
