package com.fintech.reportingservice.service;

import com.fintech.reportingservice.entity.Report;
import com.fintech.reportingservice.entity.ReportEvent;
import com.fintech.reportingservice.exception.ReportNotFoundException;
import com.fintech.reportingservice.exception.ReportServiceException;
import com.fintech.reportingservice.model.ReportFormat;
import com.fintech.reportingservice.model.ReportStatus;
import com.fintech.reportingservice.repository.ReportEventRepository;
import com.fintech.reportingservice.repository.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Worker service for async report generation, extracted to avoid @Async self-invocation.
 */
@Service
public class ReportGenerationWorker {

    private static final Logger log = LoggerFactory.getLogger(ReportGenerationWorker.class);

    private final ReportRepository reportRepository;
    private final ReportEventRepository reportEventRepository;

    public ReportGenerationWorker(ReportRepository reportRepository, ReportEventRepository reportEventRepository) {
        this.reportRepository = reportRepository;
        this.reportEventRepository = reportEventRepository;
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

    public long generateTransactionSummary(Report report) {
        log.info("Generating transaction summary report: {}", report.getReportId());
        Map<String, String> params = report.getParameters() != null ? report.getParameters() : Collections.emptyMap();
        LocalDateTime startDate = params.containsKey("startDate")
                ? LocalDateTime.parse(params.get("startDate"))
                : LocalDateTime.now().minusDays(7);
        LocalDateTime endDate = params.containsKey("endDate")
                ? LocalDateTime.parse(params.get("endDate"))
                : LocalDateTime.now();

        List<ReportEvent> events = reportEventRepository.findByEventTypeAndDateRange(
                "transaction-completed", startDate, endDate);

        report.setFileSizeBytes((long) events.size() * 256);
        return events.size();
    }

    public long generateAccountStatement(Report report) {
        log.info("Generating account statement report: {}", report.getReportId());
        Map<String, String> params = report.getParameters() != null ? report.getParameters() : Collections.emptyMap();
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

    private long generateMonthlySummary(Report report) {
        log.info("Generating monthly summary report: {}", report.getReportId());
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1);

        List<ReportEvent> events = reportEventRepository.findByDateRange(startOfMonth, endOfMonth);
        report.setFileSizeBytes((long) events.size() * 320);
        return events.size();
    }

    private long generateGenericReport(Report report) {
        log.info("Generating generic report: {}", report.getReportId());
        LocalDateTime startDate = LocalDateTime.now().minusDays(30);
        List<ReportEvent> events = reportEventRepository.findByDateRange(startDate, LocalDateTime.now());
        report.setFileSizeBytes((long) events.size() * 256);
        return events.size();
    }

    private String getFileExtension(ReportFormat format) {
        return switch (format) {
            case PDF -> ".pdf";
            case CSV -> ".csv";
            case EXCEL -> ".xlsx";
            case JSON -> ".json";
        };
    }
}
