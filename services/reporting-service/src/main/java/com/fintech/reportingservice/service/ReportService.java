package com.fintech.reportingservice.service;

import com.fintech.reportingservice.dto.request.GenerateReportRequest;
import com.fintech.reportingservice.dto.response.ReportDownloadResponse;
import com.fintech.reportingservice.dto.response.ReportResponse;
import com.fintech.reportingservice.entity.Report;
import com.fintech.reportingservice.exception.ReportNotFoundException;
import com.fintech.reportingservice.exception.ReportServiceException;
import com.fintech.reportingservice.model.ReportFormat;
import com.fintech.reportingservice.model.ReportStatus;
import com.fintech.reportingservice.model.ReportType;
import com.fintech.reportingservice.repository.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Core service handling report generation, retrieval, download, and lifecycle management.
 */
@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);
    private static final int REPORT_EXPIRY_DAYS = 30;

    private final ReportRepository reportRepository;
    private final ReportGenerationWorker reportGenerationWorker;

    public ReportService(ReportRepository reportRepository, ReportGenerationWorker reportGenerationWorker) {
        this.reportRepository = reportRepository;
        this.reportGenerationWorker = reportGenerationWorker;
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
            reportGenerationWorker.processReportGeneration(saved.getReportId());
        }

        return ReportResponse.fromEntity(saved);
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

        // Increment download count atomically
        reportRepository.incrementDownloadCount(reportId);

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

    // --- Report generation delegated to ReportGenerationWorker ---

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
