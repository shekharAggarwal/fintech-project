package com.fintech.reportingservice.controller;

import com.fintech.reportingservice.dto.request.GenerateReportRequest;
import com.fintech.reportingservice.dto.response.ReportDownloadResponse;
import com.fintech.reportingservice.dto.response.ReportResponse;
import com.fintech.reportingservice.model.ReportStatus;
import com.fintech.reportingservice.model.ReportType;
import com.fintech.reportingservice.service.ReportService;
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

/**
 * REST controller for report management operations.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Generate a new report asynchronously.
     * Returns 202 Accepted with report metadata including ID for status polling.
     */
    @PostMapping
    public ResponseEntity<ReportResponse> generateReport(
            @Valid @RequestBody GenerateReportRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "system") String userId) {

        log.info("Received report generation request: type={}, format={}, user={}",
                request.getReportType(), request.getReportFormat(), userId);

        ReportResponse response = reportService.generateReport(request, userId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    /**
     * Get report status and details by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReportResponse> getReportStatus(@PathVariable("id") String reportId) {
        ReportResponse response = reportService.getReportStatus(reportId);
        return ResponseEntity.ok(response);
    }

    /**
     * Download a completed report.
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<ReportDownloadResponse> downloadReport(@PathVariable("id") String reportId) {
        ReportDownloadResponse response = reportService.downloadReport(reportId);
        return ResponseEntity.ok(response);
    }

    /**
     * List reports with optional filtering and pagination.
     */
    @GetMapping
    public ResponseEntity<Page<ReportResponse>> listReports(
            @RequestParam(value = "type", required = false) ReportType type,
            @RequestParam(value = "status", required = false) ReportStatus status,
            @RequestParam(value = "createdBy", required = false) String createdBy,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ReportResponse> reports = reportService.listReports(type, status, createdBy, pageable);
        return ResponseEntity.ok(reports);
    }

    /**
     * Delete (cancel) a report.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReport(@PathVariable("id") String reportId) {
        log.info("Received delete request for report: {}", reportId);
        reportService.deleteReport(reportId);
        return ResponseEntity.noContent().build();
    }
}
