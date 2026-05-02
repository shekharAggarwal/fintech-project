package com.fintech.transactionservice.controller;

import com.fintech.transactionservice.dto.request.TransactionRequest;
import com.fintech.transactionservice.dto.response.TransactionResponse;
import com.fintech.transactionservice.entity.Transaction;
import com.fintech.transactionservice.entity.TransactionStatus;
import com.fintech.transactionservice.exception.InvalidTransactionException;
import com.fintech.transactionservice.exception.TransactionNotFoundException;
import com.fintech.transactionservice.service.IdempotencyService;
import com.fintech.transactionservice.service.TransactionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionService transactionService;
    private final IdempotencyService idempotencyService;

    public TransactionController(TransactionService transactionService, IdempotencyService idempotencyService) {
        this.transactionService = transactionService;
        this.idempotencyService = idempotencyService;
    }

    /**
     * POST /api/transactions — Initiate a new transaction.
     * Returns 201 Created on success, or existing transaction if idempotency key matches.
     */
    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @Valid @RequestBody TransactionRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        // Validate idempotency key from header or body
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new InvalidTransactionException("Idempotency-Key header is required", "Idempotency-Key");
        }

        // Check for duplicate via idempotency service
        Optional<Transaction> existing = idempotencyService.checkDuplicate(idempotencyKey);
        if (existing.isPresent()) {
            logger.warn("Duplicate idempotency key detected: {}", sanitizeLogInput(idempotencyKey));
            TransactionResponse conflictResponse = toResponse(existing.get());
            conflictResponse.setDescription("Duplicate transaction: idempotency key already used");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(conflictResponse);
        }

        // Initiate new transaction
        Transaction transaction = transactionService.initiateTransaction(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(transaction));
    }

    /**
     * GET /api/transactions/{id} — Get transaction by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable String id) {
        Transaction transaction = transactionService.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
        return ResponseEntity.ok(toResponse(transaction));
    }

    /**
     * GET /api/transactions?accountId=&status= — Filter transactions.
     */
    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @RequestParam(required = false) String accountId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        TransactionStatus statusEnum = null;
        if (status != null && !status.isBlank()) {
            try {
                statusEnum = TransactionStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidTransactionException("Invalid status value: " + status, "status");
            }
        }

        // Cap page size to prevent unbounded queries
        int cappedSize = Math.min(size, 200);
        Pageable pageable = PageRequest.of(page, cappedSize);

        Page<Transaction> transactions = transactionService.findTransactions(accountId, statusEnum, pageable);
        List<TransactionResponse> responses = transactions.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * GET /api/transactions/{id}/status — Polling endpoint for transaction status.
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<TransactionResponse> getTransactionStatus(@PathVariable String id) {
        Transaction transaction = transactionService.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));

        TransactionResponse response = new TransactionResponse();
        response.setTransactionId(transaction.getTxnId());
        response.setStatus(transaction.getStatus());
        response.setFailureReason(transaction.getFailureReason());
        response.setUpdatedAt(transaction.getUpdatedAt());
        return ResponseEntity.ok(response);
    }

    private String sanitizeLogInput(String input) {
        if (input == null) return "null";
        return input.replaceAll("[\\n\\r\\t]", "_");
    }

    private TransactionResponse toResponse(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();
        response.setTransactionId(transaction.getTxnId());
        response.setPaymentId(transaction.getPaymentId());
        response.setPayerId(transaction.getFromAccount());
        response.setReceiverId(transaction.getToAccount());
        response.setAmount(transaction.getAmount());
        response.setStatus(transaction.getStatus());
        response.setDescription(transaction.getDescription());
        response.setFailureReason(transaction.getFailureReason());
        response.setRetryCount(transaction.getRetryCount());
        response.setCreatedAt(transaction.getCreatedAt());
        response.setUpdatedAt(transaction.getUpdatedAt());
        response.setProcessedAt(transaction.getCompletedAt());
        return response;
    }
}
