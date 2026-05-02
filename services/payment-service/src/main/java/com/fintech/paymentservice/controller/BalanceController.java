package com.fintech.paymentservice.controller;

import com.fintech.paymentservice.dto.request.BalanceOperationRequest;
import com.fintech.paymentservice.dto.response.BalanceResponse;
import com.fintech.paymentservice.service.BalanceService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class BalanceController {

    private static final Logger logger = LoggerFactory.getLogger(BalanceController.class);

    private final BalanceService balanceService;

    public BalanceController(BalanceService balanceService) {
        this.balanceService = balanceService;
    }

    /**
     * Get account balance
     */
    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<?> getBalance(@PathVariable String accountNumber) {
        logger.info("Getting balance for account: {}", accountNumber);
        try {
            BalanceResponse response = balanceService.getBalance(accountNumber);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get balance for account {}: {}", accountNumber, e.getMessage());
            return ResponseEntity.status(404)
                .body(Map.of("error", "Account not found", "account", accountNumber));
        }
    }

    /**
     * Credit funds to an account
     */
    @PostMapping("/{accountNumber}/credit")
    public ResponseEntity<?> credit(
            @PathVariable String accountNumber,
            @Valid @RequestBody BalanceOperationRequest request) {
        logger.info("Credit request for account {}: amount={}", accountNumber, request.amount());
        try {
            BalanceResponse response = balanceService.credit(accountNumber, request.amount(), request.description());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to credit account {}: {}", accountNumber, e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Credit failed", "reason", e.getMessage()));
        }
    }

    /**
     * Debit funds from an account
     */
    @PostMapping("/{accountNumber}/debit")
    public ResponseEntity<?> debit(
            @PathVariable String accountNumber,
            @Valid @RequestBody BalanceOperationRequest request) {
        logger.info("Debit request for account {}: amount={}", accountNumber, request.amount());
        try {
            BalanceResponse response = balanceService.debit(accountNumber, request.amount(), request.description());
            return ResponseEntity.ok(response);
        } catch (com.fintech.paymentservice.exception.InsufficientFundsException e) {
            return ResponseEntity.status(409)
                .body(Map.of("error", "Insufficient funds", "reason", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to debit account {}: {}", accountNumber, e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Debit failed", "reason", e.getMessage()));
        }
    }

    /**
     * Place a hold on funds
     */
    @PostMapping("/{accountNumber}/hold")
    public ResponseEntity<?> placeHold(
            @PathVariable String accountNumber,
            @Valid @RequestBody BalanceOperationRequest request) {
        logger.info("Hold request for account {}: amount={}", accountNumber, request.amount());
        try {
            BalanceResponse response = balanceService.placeHold(accountNumber, request.amount(), request.description());
            return ResponseEntity.ok(response);
        } catch (com.fintech.paymentservice.exception.InsufficientFundsException e) {
            return ResponseEntity.status(409)
                .body(Map.of("error", "Insufficient funds for hold", "reason", e.getMessage()));
        } catch (Exception e) {
            logger.error("Failed to place hold on account {}: {}", accountNumber, e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Hold failed", "reason", e.getMessage()));
        }
    }

    /**
     * Release a hold on funds
     */
    @PostMapping("/{accountNumber}/release-hold")
    public ResponseEntity<?> releaseHold(
            @PathVariable String accountNumber,
            @Valid @RequestBody BalanceOperationRequest request) {
        logger.info("Release hold request for account {}: amount={}", accountNumber, request.amount());
        try {
            BalanceResponse response = balanceService.releaseHold(accountNumber, request.amount(), request.description());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to release hold on account {}: {}", accountNumber, e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Release hold failed", "reason", e.getMessage()));
        }
    }
}
