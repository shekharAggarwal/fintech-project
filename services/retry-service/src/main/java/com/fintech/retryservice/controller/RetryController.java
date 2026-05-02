package com.fintech.retryservice.controller;

import com.fintech.retryservice.dto.RetryRequest;
import com.fintech.retryservice.dto.RetryResponse;
import com.fintech.retryservice.dto.RetryStatusUpdate;
import com.fintech.retryservice.model.RetryStatus;
import com.fintech.retryservice.service.RetryService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.NoSuchElementException;

/**
 * REST controller for retry management operations.
 */
@RestController
@RequestMapping("/api/retries")
public class RetryController {

    private static final Logger logger = LoggerFactory.getLogger(RetryController.class);

    private final RetryService retryService;

    public RetryController(RetryService retryService) {
        this.retryService = retryService;
    }

    /**
     * GET /api/retries?status=&page=&size=&sort=
     * List retry attempts with optional status filter and pagination.
     */
    @GetMapping
    public ResponseEntity<Page<RetryResponse>> getRetries(
            @RequestParam(required = false) RetryStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") String direction) {

        Sort sortOrder = Sort.by(Sort.Direction.fromString(direction), sort);
        Pageable pageable = PageRequest.of(page, size, sortOrder);

        Page<RetryResponse> result;
        if (status != null) {
            result = retryService.getRetriesByStatus(status, pageable);
        } else {
            result = retryService.getAllRetries(pageable);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/retries/{id}
     * Get a single retry attempt by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RetryResponse> getRetryById(@PathVariable String id) {
        try {
            RetryResponse response = retryService.getRetryById(id);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/retries
     * Create a new retry attempt.
     */
    @PostMapping
    public ResponseEntity<RetryResponse> createRetry(@Valid @RequestBody RetryRequest request) {
        logger.info("Creating retry for originalId={}, type={}", request.getOriginalId(), request.getRetryType());
        RetryResponse response = retryService.scheduleRetry(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/retries/{id}/retry-now
     * Force immediate retry execution.
     */
    @PostMapping("/{id}/retry-now")
    public ResponseEntity<RetryResponse> forceRetryNow(@PathVariable String id) {
        try {
            RetryResponse response = retryService.forceRetryNow(id);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * DELETE /api/retries/{id}
     * Cancel a retry attempt.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<RetryResponse> cancelRetry(@PathVariable String id) {
        try {
            RetryResponse response = retryService.cancelRetry(id);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * PUT /api/retries/{id}/status
     * Update retry status (callback endpoint for downstream services).
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<RetryResponse> updateStatus(
            @PathVariable String id,
            @RequestBody RetryStatusUpdate statusUpdate) {
        try {
            RetryResponse response = retryService.updateStatus(id, statusUpdate);
            return ResponseEntity.ok(response);
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * GET /api/retries/statistics
     * Get retry statistics summary.
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = retryService.getStatistics();
        return ResponseEntity.ok(stats);
    }
}
