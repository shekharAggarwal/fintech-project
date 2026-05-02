package com.fintech.transactionservice.controller;

import com.fintech.transactionservice.dto.request.DisputeRequest;
import com.fintech.transactionservice.dto.response.DisputeResponse;
import com.fintech.transactionservice.entity.Dispute;
import com.fintech.transactionservice.entity.DisputeStatus;
import com.fintech.transactionservice.service.DisputeService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/disputes")
public class DisputeController {

    private static final Logger logger = LoggerFactory.getLogger(DisputeController.class);

    private final DisputeService disputeService;

    public DisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    /**
     * POST /api/disputes — Open a new dispute.
     */
    @PostMapping
    public ResponseEntity<DisputeResponse> openDispute(@Valid @RequestBody DisputeRequest request) {
        logger.info("Opening dispute for transaction: {}", request.getTransactionId());
        Dispute dispute = disputeService.openDispute(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(dispute));
    }

    /**
     * GET /api/disputes/{id} — Get dispute by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DisputeResponse> getDispute(@PathVariable String id) {
        Dispute dispute = disputeService.getDispute(id);
        return ResponseEntity.ok(toResponse(dispute));
    }

    /**
     * GET /api/disputes?transactionId= — Get disputes by transaction.
     */
    @GetMapping
    public ResponseEntity<List<DisputeResponse>> getDisputes(
            @RequestParam(required = false) String transactionId) {
        List<Dispute> disputes;
        if (transactionId != null) {
            disputes = disputeService.getDisputesByTransaction(transactionId);
        } else {
            disputes = disputeService.getDisputesByTransaction(null);
        }
        return ResponseEntity.ok(disputes.stream().map(this::toResponse).collect(Collectors.toList()));
    }

    /**
     * PUT /api/disputes/{id}/review — Move dispute to UNDER_REVIEW.
     */
    @PutMapping("/{id}/review")
    public ResponseEntity<DisputeResponse> reviewDispute(@PathVariable String id) {
        Dispute dispute = disputeService.reviewDispute(id);
        return ResponseEntity.ok(toResponse(dispute));
    }

    /**
     * PUT /api/disputes/{id}/resolve — Resolve a dispute.
     */
    @PutMapping("/{id}/resolve")
    public ResponseEntity<DisputeResponse> resolveDispute(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        String resolution = body.getOrDefault("resolution", "");
        String statusStr = body.getOrDefault("status", "RESOLVED_APPROVED");
        DisputeStatus resolvedStatus = DisputeStatus.valueOf(statusStr);
        Dispute dispute = disputeService.resolveDispute(id, resolution, resolvedStatus);
        return ResponseEntity.ok(toResponse(dispute));
    }

    /**
     * DELETE /api/disputes/{id} — Cancel a dispute.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<DisputeResponse> cancelDispute(@PathVariable String id) {
        Dispute dispute = disputeService.cancelDispute(id);
        return ResponseEntity.ok(toResponse(dispute));
    }

    private DisputeResponse toResponse(Dispute dispute) {
        DisputeResponse response = new DisputeResponse();
        response.setId(dispute.getId());
        response.setTransactionId(dispute.getTransactionId());
        response.setDisputeType(dispute.getDisputeType());
        response.setStatus(dispute.getStatus());
        response.setReason(dispute.getReason());
        response.setEvidence(dispute.getEvidence());
        response.setRefundAmount(dispute.getRefundAmount());
        response.setResolution(dispute.getResolution());
        response.setCreatedAt(dispute.getCreatedAt());
        response.setUpdatedAt(dispute.getUpdatedAt());
        response.setResolvedAt(dispute.getResolvedAt());
        return response;
    }
}
