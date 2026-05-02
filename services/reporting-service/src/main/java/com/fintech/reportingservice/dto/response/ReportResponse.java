package com.fintech.reportingservice.dto.response;

import com.fintech.reportingservice.entity.Report;
import com.fintech.reportingservice.model.ReportFormat;
import com.fintech.reportingservice.model.ReportStatus;
import com.fintech.reportingservice.model.ReportType;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Response DTO for report data.
 */
public class ReportResponse {

    private String reportId;
    private String reportName;
    private ReportType reportType;
    private ReportStatus reportStatus;
    private ReportFormat reportFormat;
    private String description;
    private Map<String, String> parameters;
    private Long fileSizeBytes;
    private Long recordCount;
    private Long generationTimeMs;
    private String createdBy;
    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String errorMessage;
    private Integer downloadCount;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean readyForDownload;

    /**
     * Maps a Report entity to a ReportResponse DTO.
     */
    public static ReportResponse fromEntity(Report report) {
        ReportResponse response = new ReportResponse();
        response.setReportId(report.getReportId());
        response.setReportName(report.getReportName());
        response.setReportType(report.getReportType());
        response.setReportStatus(report.getReportStatus());
        response.setReportFormat(report.getReportFormat());
        response.setDescription(report.getDescription());
        response.setParameters(report.getParameters());
        response.setFileSizeBytes(report.getFileSizeBytes());
        response.setRecordCount(report.getRecordCount());
        response.setGenerationTimeMs(report.getGenerationTimeMs());
        response.setCreatedBy(report.getCreatedBy());
        response.setScheduledAt(report.getScheduledAt());
        response.setStartedAt(report.getStartedAt());
        response.setCompletedAt(report.getCompletedAt());
        response.setErrorMessage(report.getErrorMessage());
        response.setDownloadCount(report.getDownloadCount());
        response.setExpiresAt(report.getExpiresAt());
        response.setCreatedAt(report.getCreatedAt());
        response.setUpdatedAt(report.getUpdatedAt());
        response.setReadyForDownload(report.isReadyForDownload());
        return response;
    }

    // --- Getters and Setters ---

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public String getReportName() { return reportName; }
    public void setReportName(String reportName) { this.reportName = reportName; }

    public ReportType getReportType() { return reportType; }
    public void setReportType(ReportType reportType) { this.reportType = reportType; }

    public ReportStatus getReportStatus() { return reportStatus; }
    public void setReportStatus(ReportStatus reportStatus) { this.reportStatus = reportStatus; }

    public ReportFormat getReportFormat() { return reportFormat; }
    public void setReportFormat(ReportFormat reportFormat) { this.reportFormat = reportFormat; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Map<String, String> getParameters() { return parameters; }
    public void setParameters(Map<String, String> parameters) { this.parameters = parameters; }

    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public Long getRecordCount() { return recordCount; }
    public void setRecordCount(Long recordCount) { this.recordCount = recordCount; }

    public Long getGenerationTimeMs() { return generationTimeMs; }
    public void setGenerationTimeMs(Long generationTimeMs) { this.generationTimeMs = generationTimeMs; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Integer getDownloadCount() { return downloadCount; }
    public void setDownloadCount(Integer downloadCount) { this.downloadCount = downloadCount; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isReadyForDownload() { return readyForDownload; }
    public void setReadyForDownload(boolean readyForDownload) { this.readyForDownload = readyForDownload; }
}
