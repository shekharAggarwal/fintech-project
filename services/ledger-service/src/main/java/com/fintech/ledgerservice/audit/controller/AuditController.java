package com.fintech.ledgerservice.audit.controller;

import com.fintech.ledgerservice.audit.dto.AuditSearchCriteria;
import com.fintech.ledgerservice.audit.entity.AuditEvent;
import com.fintech.ledgerservice.audit.entity.AuditEventType;
import com.fintech.ledgerservice.audit.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Admin-only REST controller for querying the immutable audit trail.
 */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private static final Logger logger = LoggerFactory.getLogger(AuditController.class);

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * GET /api/audit/trail — returns paginated audit trail (admin only).
     */
    @GetMapping("/trail")
    public ResponseEntity<Page<AuditEvent>> getAuditTrail(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        logger.info("GET /api/audit/trail?page={}&size={}", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return ResponseEntity.ok(auditService.getAuditTrail(pageable));
    }

    /**
     * GET /api/audit/actor/{actorId} — returns audit events by actor.
     */
    @GetMapping("/actor/{actorId}")
    public ResponseEntity<Page<AuditEvent>> getAuditByActor(
            @PathVariable String actorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        logger.info("GET /api/audit/actor/{}", actorId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return ResponseEntity.ok(auditService.getAuditByActor(actorId, pageable));
    }

    /**
     * GET /api/audit/type/{eventType} — returns audit events by type.
     */
    @GetMapping("/type/{eventType}")
    public ResponseEntity<Page<AuditEvent>> getAuditByType(
            @PathVariable AuditEventType eventType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        logger.info("GET /api/audit/type/{}", eventType);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return ResponseEntity.ok(auditService.getAuditByType(eventType, pageable));
    }

    /**
     * GET /api/audit/range?from=&to= — returns audit events within a date range.
     */
    @GetMapping("/range")
    public ResponseEntity<Page<AuditEvent>> getAuditByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        logger.info("GET /api/audit/range?from={}&to={}", from, to);

        Instant startDate = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant endDate = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return ResponseEntity.ok(auditService.getAuditByDateRange(startDate, endDate, pageable));
    }

    /**
     * POST /api/audit/search — advanced search with multiple criteria.
     */
    @PostMapping("/search")
    public ResponseEntity<Page<AuditEvent>> searchAudit(
            @RequestBody AuditSearchCriteria criteria,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        logger.info("POST /api/audit/search");
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        return ResponseEntity.ok(auditService.searchAudit(criteria, pageable));
    }
}
