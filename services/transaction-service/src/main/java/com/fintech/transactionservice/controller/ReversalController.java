package com.fintech.transactionservice.controller;

import com.fintech.transactionservice.dto.request.ReversalRequest;
import com.fintech.transactionservice.dto.response.ReversalResponse;
import com.fintech.transactionservice.entity.Reversal;
import com.fintech.transactionservice.service.DisputeService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reversals")
public class ReversalController {

    private static final Logger logger = LoggerFactory.getLogger(ReversalController.class);

    private final DisputeService disputeService;

    public ReversalController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    /**
     * POST /api/reversals — Initiate a reversal.
     */
    @PostMapping
    public ResponseEntity<ReversalResponse> initiateReversal(@Valid @RequestBody ReversalRequest request) {
        logger.info("Initiating reversal for transaction: {}", request.getOriginalTransactionId());
        Reversal reversal = disputeService.initiateReversal(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(reversal));
    }

    /**
     * GET /api/reversals/{id} — Get reversal by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReversalResponse> getReversal(@PathVariable String id) {
        Reversal reversal = disputeService.getReversal(id);
        return ResponseEntity.ok(toResponse(reversal));
    }

    /**
     * GET /api/reversals?transactionId= — Get reversals for a transaction.
     */
    @GetMapping
    public ResponseEntity<List<ReversalResponse>> getReversals(
            @RequestParam(required = false) String transactionId) {
        List<Reversal> reversals;
        if (transactionId != null) {
            reversals = disputeService.getReversalsByTransaction(transactionId);
        } else {
            reversals = disputeService.getReversalsByTransaction(null);
        }
        return ResponseEntity.ok(reversals.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    private ReversalResponse toResponse(Reversal reversal) {
        ReversalResponse response = new ReversalResponse();
        response.setId(reversal.getId());
        response.setOriginalTransactionId(reversal.getOriginalTransactionId());
        response.setReversalTransactionId(reversal.getReversalTransactionId());
        response.setReversalType(reversal.getReversalType());
        response.setAmount(reversal.getAmount());
        response.setStatus(reversal.getStatus());
        response.setReason(reversal.getReason());
        response.setCreatedAt(reversal.getCreatedAt());
        response.setUpdatedAt(reversal.getUpdatedAt());
        response.setCompletedAt(reversal.getCompletedAt());
        return response;
    }
}
