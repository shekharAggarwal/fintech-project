package com.fintech.reportingservice.service;

import com.fintech.reportingservice.dto.request.GenerateReportRequest;
import com.fintech.reportingservice.dto.response.ReportDownloadResponse;
import com.fintech.reportingservice.dto.response.ReportResponse;
import com.fintech.reportingservice.entity.Report;
import com.fintech.reportingservice.entity.ReportEvent;
import com.fintech.reportingservice.exception.ReportNotFoundException;
import com.fintech.reportingservice.exception.ReportServiceException;
import com.fintech.reportingservice.model.ReportFormat;
import com.fintech.reportingservice.model.ReportStatus;
import com.fintech.reportingservice.model.ReportType;
import com.fintech.reportingservice.repository.ReportEventRepository;
import com.fintech.reportingservice.repository.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Core service handling report generation, retrieval, download, and lifecycle management.
 */
@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);
    private static final int REPORT_EXPIRY_DAYS = 30;

    private final ReportRepository reportRepository;
    private final ReportEventRepository reportEventRepository;

    public ReportService(ReportRepository reportRepository, ReportEventRepository reportEventRepository) {
        this.reportRepository = reportRepository;
        this.reportEventRepository = reportEventRepository;
    }

    /**
     * Initiates report generation asynchronously. Returns the report metadata immediately.
     */
    @Transactional
    public ReportResponse generateReport(GenerateReportRequest request, String userId) {
        log.info("Generating report '{}' of type {} for user {}", request.getReportName(), request.getReportType(), userId);

        Report report = new Report();
        report.setReportId(UUID.randomUUID().toString());
        report.setReportName(request.getReportName());
        report.setReportType(request.getReportType());
        report.setReportFormat(request.getReportFormat());
        report.setDescription(request.getDescription());
        report.setCreatedBy(userId);
        report.setReportStatus(ReportStatus.PENDING);

        if (request.getParameters() != null) {
            report.setParameters(request.getParameters());
        }

        if (request.getScheduledAt() != null && request.getScheduledAt().isAfter(LocalDateTime.now())) {
            report.setScheduledAt(request.getScheduledAt());
            report.setReportStatus(ReportStatus.SCHEDULED);
        }

        report.setExpiresAt(LocalDateTime.now().plusDays(REPORT_EXPIRY_DAYS));
        Report saved = reportRepository.save(report);

        // Trigger async generation if not scheduled
        if (saved.getReportStatus() == ReportStatus.PENDING) {
            processReportGeneration(saved.getReportId());
        }

        return ReportResponse.fromEntity(saved);
    }

    /**
     * Asynchronously processes report generation based on report type.
     */
    @Async
    @Transactional
    public void processReportGeneration(String reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException(reportId));

        report.setReportStatus(ReportStatus.IN_PROGRESS);
        report.setStartedAt(LocalDateTime.now());
        reportRepository.save(report);

        long startTime = System.currentTimeMillis();

        try {
            long recordCount = switch (report.getReportType()) {
                case TRANSACTION_SUMMARY -> generateTransactionSummary(report);
                case ACCOUNT_STATEMENT -> generateAccountStatement(report);
                case DAILY_RECONCILIATION -> generateDailyReconciliation(report);
                case MONTHLY_SUMMARY -> generateMonthlySummary(report);
                default -> generateGenericReport(report);
            };

            long generationTime = System.currentTimeMillis() - startTime;

            report.setReportStatus(ReportStatus.COMPLETED);
            report.setCompletedAt(LocalDateTime.now());
            report.setGenerationTimeMs(generationTime);
            report.setRecordCount(recordCount);
            report.setFilePath("/reports/" + report.getReportId() + getFileExtension(report.getReportFormat()));
            reportRepository.save(report);

            log.info("Report {} generated successfully in {}ms with {} records",
                    reportId, generationTime, recordCount);
        } catch (Exception e) {
            log.error("Failed to generate report {}: {}", reportId, e.getMessage(), e);
            report.setReportStatus(ReportStatus.FAILED);
            report.setErrorMessage(e.getMessage());
            report.setCompletedAt(LocalDateTime.now());
            report.setGenerationTimeMs(System.currentTimeMillis() - startTime);
            reportRepository.save(report);
        }
    }

    /**
     * Gets current report status.
     */
    @Transactional(readOnly = true)
    public ReportResponse getReportStatus(String reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException(reportId));
        return ReportResponse.fromEntity(report);
    }

    /**
     * Downloads a completed report.
     */
    @Transactional
    public ReportDownloadResponse downloadReport(String reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException(reportId));

        if (!report.isReadyForDownload()) {
            if (report.isExpired()) {
                throw new ReportServiceException("Report has expired", "REPORT_EXPIRED");
            }
            if (report.getReportStatus() != ReportStatus.COMPLETED) {
                throw new ReportServiceException(
                        "Report is not ready for download. Current status: " + report.getReportStatus(),
                        "REPORT_NOT_READY");
            }
            throw new ReportServiceException("Report file is not available", "FILE_UNAVAILABLE");
        }

        // Increment download count
        report.setDownloadCount(report.getDownloadCount() + 1);
        reportRepository.save(report);

        ReportDownloadResponse response = new ReportDownloadResponse();
        response.setReportId(report.getReportId());
        response.setReportName(report.getReportName());
        response.setFileName(report.getReportName() + getFileExtension(report.getReportFormat()));
        response.setContentType(getContentType(report.getReportFormat()));
        response.setFileSizeBytes(report.getFileSizeBytes());
        response.setDownloadUrl(report.getFilePath());
        return response;
    }

    /**
     * Lists reports with optional filtering by type, status, and creator.
     */
    @Transactional(readOnly = true)
    public Page<ReportResponse> listReports(ReportType type, ReportStatus status, String createdBy, Pageable pageable) {
        Page<Report> reports = reportRepository.findByFilters(type, status, createdBy, pageable);
        return reports.map(ReportResponse::fromEntity);
    }

    /**
     * Soft-deletes a report by cancelling it.
     */
    @Transactional
    public void deleteReport(String reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ReportNotFoundException(reportId));

        if (report.getReportStatus() == ReportStatus.IN_PROGRESS) {
            throw new ReportServiceException("Cannot delete a report that is currently being generated", "DELETE_IN_PROGRESS");
        }

        report.setReportStatus(ReportStatus.CANCELLED);
        reportRepository.save(report);
        log.info("Report {} marked as cancelled", reportId);
    }

    // --- Report generation methods ---

    /**
     * Generates a transaction summary report from event store data.
     */
    public long generateTransactionSummary(Report report) {
        log.info("Generating transaction summary report: {}", report.getReportId());
        Map<String, String> params = report.getParameters();
        LocalDateTime startDate = params.containsKey("startDate")
                ? LocalDateTime.parse(params.get("startDate"))
                : LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = params.containsKey("endDate")
                ? LocalDateTime.parse(params.get("endDate"))
                : LocalDateTime.now();

        List<ReportEvent> events = reportEventRepository.findByEventTypeAndDateRange(
                "transaction-completed", startDate, endDate);

        // Simulate file size based on record count
        report.setFileSizeBytes((long) events.size() * 256);
        return events.size();
    }

    /**
     * Generates an account statement report for a specific account.
     */
    public long generateAccountStatement(Report report) {
        log.info("Generating account statement report: {}", report.getReportId());
        Map<String, String> params = report.getParameters();
        String accountId = params.get("accountId");
        if (accountId == null || accountId.isBlank()) {
            throw new ReportServiceException("accountId parameter is required for ACCOUNT_STATEMENT reports", "MISSING_PARAMETER");
        }

        LocalDateTime startDate = params.containsKey("startDate")
                ? LocalDateTime.parse(params.get("startDate"))
                : LocalDateTime.now().minusMonths(1);
        LocalDateTime endDate = params.containsKey("endDate")
                ? LocalDateTime.parse(params.get("endDate"))
                : LocalDateTime.now();

        List<ReportEvent> events = reportEventRepository.findByAccountIdAndDateRange(accountId, startDate, endDate);
        report.setFileSizeBytes((long) events.size() * 512);
        return events.size();
    }

    /**
     * Generates a daily reconciliation report.
     */
    public long generateDailyReconciliation(Report report) {
        log.info("Generating daily reconciliation report: {}", report.getReportId());
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        List<ReportEvent> completedEvents = reportEventRepository.findByEventTypeAndDateRange(
                "transaction-completed", startOfDay, endOfDay);
        List<ReportEvent> failedEvents = reportEventRepository.findByEventTypeAndDateRange(
                "transaction-failed", startOfDay, endOfDay);

        long totalRecords = completedEvents.size() + failedEvents.size();
        report.setFileSizeBytes(totalRecords * 384);
        return totalRecords;
    }

    /**
     * Generates a monthly summary report.
     */
    private long generateMonthlySummary(Report report) {
        log.info("Generating monthly summary report: {}", report.getReportId());
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1);

        List<ReportEvent> events = reportEventRepository.findByDateRange(startOfMonth, endOfMonth);
        report.setFileSizeBytes((long) events.size() * 320);
        return events.size();
    }

    /**
     * Generates a generic report for unsupported/custom types.
     */
    private long generateGenericReport(Report report) {
        log.info("Generating generic report: {}", report.getReportId());
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        List<ReportEvent> events = reportEventRepository.findByDateRange(startDate, LocalDateTime.now());
        report.setFileSizeBytes((long) events.size() * 256);
        return events.size();
    }

    // --- Utility methods ---

    private String getFileExtension(ReportFormat format) {
        return switch (format) {
            case PDF -> ".pdf";
            case CSV -> ".csv";
            case EXCEL -> ".xlsx";
            case JSON -> ".json";
        };
    }

    private String getContentType(ReportFormat format) {
        return switch (format) {
            case PDF -> "application/pdf";
            case CSV -> "text/csv";
            case EXCEL -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case JSON -> "application/json";
        };
    }
}
