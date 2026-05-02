package com.fintech.paymentservice.fraud.controller;

import com.fintech.paymentservice.fraud.entity.FraudAlert;
import com.fintech.paymentservice.fraud.model.AlertStatus;
import com.fintech.paymentservice.fraud.model.AlertType;
import com.fintech.paymentservice.fraud.repository.FraudAlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/fraud")
public class FraudController {

    private static final Logger logger = LoggerFactory.getLogger(FraudController.class);

    private final FraudAlertRepository alertRepository;

    public FraudController(FraudAlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    /**
     * List all fraud alerts with pagination
     */
    @GetMapping("/alerts")
    public ResponseEntity<Page<FraudAlert>> listAlerts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        Pageable pageable = PageRequest.of(page, size);

        Page<FraudAlert> alerts;
        if (status != null) {
            alerts = alertRepository.findByStatus(AlertStatus.valueOf(status.toUpperCase()), pageable);
        } else {
            alerts = alertRepository.findAllByOrderByCreatedAtDesc(pageable);
        }

        return ResponseEntity.ok(alerts);
    }

    /**
     * Get a specific fraud alert
     */
    @GetMapping("/alerts/{alertId}")
    public ResponseEntity<?> getAlert(@PathVariable Long alertId) {
        Optional<FraudAlert> alert = alertRepository.findById(alertId);
        if (alert.isPresent()) {
            return ResponseEntity.ok(alert.get());
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * Get fraud alerts for a specific payment
     */
    @GetMapping("/alerts/payment/{paymentId}")
    public ResponseEntity<?> getAlertsByPayment(@PathVariable String paymentId) {
        return ResponseEntity.ok(alertRepository.findByPaymentId(paymentId));
    }

    /**
     * Get fraud alerts for a specific account
     */
    @GetMapping("/alerts/account/{accountId}")
    public ResponseEntity<?> getAlertsByAccount(@PathVariable String accountId) {
        return ResponseEntity.ok(alertRepository.findByAccountId(accountId));
    }

    /**
     * Update alert status (resolve, confirm fraud, mark as false positive)
     */
    @PutMapping("/alerts/{alertId}/status")
    public ResponseEntity<?> updateAlertStatus(
            @PathVariable Long alertId,
            @RequestBody Map<String, String> body) {

        Optional<FraudAlert> alertOpt = alertRepository.findById(alertId);
        if (alertOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        FraudAlert alert = alertOpt.get();
        String newStatus = body.get("status");
        String resolvedBy = body.get("resolvedBy");
        String notes = body.get("notes");

        try {
            alert.setStatus(AlertStatus.valueOf(newStatus.toUpperCase()));
            if (resolvedBy != null) alert.setResolvedBy(resolvedBy);
            if (notes != null) alert.setResolutionNotes(notes);
            alert.setResolvedAt(Instant.now());

            alertRepository.save(alert);
            logger.info("Fraud alert {} status updated to {} by {}", alertId, newStatus, resolvedBy);

            return ResponseEntity.ok(alert);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid status", "validStatuses", AlertStatus.values()));
        }
    }

    /**
     * Get fraud statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        long totalAlerts = alertRepository.count();
        long openAlerts = alertRepository.findByStatus(AlertStatus.OPEN, PageRequest.of(0, 1)).getTotalElements();
        long confirmedFraud = alertRepository.findByStatus(AlertStatus.CONFIRMED_FRAUD, PageRequest.of(0, 1)).getTotalElements();
        long falsePositives = alertRepository.findByStatus(AlertStatus.FALSE_POSITIVE, PageRequest.of(0, 1)).getTotalElements();

        return ResponseEntity.ok(Map.of(
            "totalAlerts", totalAlerts,
            "openAlerts", openAlerts,
            "confirmedFraud", confirmedFraud,
            "falsePositives", falsePositives
        ));
    }
}
