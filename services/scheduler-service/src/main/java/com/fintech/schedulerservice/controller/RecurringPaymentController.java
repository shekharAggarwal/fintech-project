package com.fintech.schedulerservice.controller;

import com.fintech.schedulerservice.dto.RecurringPaymentRequest;
import com.fintech.schedulerservice.dto.RecurringPaymentResponse;
import com.fintech.schedulerservice.dto.RecurringPaymentUpdateRequest;
import com.fintech.schedulerservice.service.RecurringPaymentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for recurring payment endpoints
 */
@RestController
@RequestMapping("/api/recurring-payments")
public class RecurringPaymentController {

    private final RecurringPaymentService recurringPaymentService;

    public RecurringPaymentController(RecurringPaymentService recurringPaymentService) {
        this.recurringPaymentService = recurringPaymentService;
    }

    /**
     * Create a new recurring payment
     */
    @PostMapping
    public ResponseEntity<RecurringPaymentResponse> createRecurringPayment(
            @Valid @RequestBody RecurringPaymentRequest request) {
        RecurringPaymentResponse response = recurringPaymentService.createRecurringPayment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all recurring payments with optional user filter
     */
    @GetMapping
    public ResponseEntity<Page<RecurringPaymentResponse>> getRecurringPayments(
            @RequestParam(required = false) String userId,
            Pageable pageable) {
        if (userId != null) {
            Page<RecurringPaymentResponse> payments = recurringPaymentService.getPaymentsByUserId(userId, pageable);
            return ResponseEntity.ok(payments);
        }
        Page<RecurringPaymentResponse> payments = recurringPaymentService.getAllPayments(pageable);
        return ResponseEntity.ok(payments);
    }

    /**
     * Get a recurring payment by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<RecurringPaymentResponse> getRecurringPaymentById(@PathVariable String id) {
        RecurringPaymentResponse response = recurringPaymentService.getPaymentById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Update a recurring payment
     */
    @PutMapping("/{id}")
    public ResponseEntity<RecurringPaymentResponse> updateRecurringPayment(
            @PathVariable String id,
            @Valid @RequestBody RecurringPaymentUpdateRequest request) {
        RecurringPaymentResponse response = recurringPaymentService.updatePayment(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Pause a recurring payment
     */
    @PostMapping("/{id}/pause")
    public ResponseEntity<RecurringPaymentResponse> pauseRecurringPayment(@PathVariable String id) {
        RecurringPaymentResponse response = recurringPaymentService.pausePayment(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Resume a paused recurring payment
     */
    @PostMapping("/{id}/resume")
    public ResponseEntity<RecurringPaymentResponse> resumeRecurringPayment(@PathVariable String id) {
        RecurringPaymentResponse response = recurringPaymentService.resumePayment(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel/delete a recurring payment
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> cancelRecurringPayment(@PathVariable String id) {
        recurringPaymentService.cancelPayment(id);
        return ResponseEntity.noContent().build();
    }
}
