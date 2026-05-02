package com.fintech.reportingservice.dto.request;

import com.fintech.reportingservice.model.ReportFormat;
import com.fintech.reportingservice.model.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Request DTO for generating a new report.
 */
public class GenerateReportRequest {

    @NotBlank(message = "Report name is required")
    private String reportName;

    @NotNull(message = "Report type is required")
    private ReportType reportType;

    @NotNull(message = "Report format is required")
    private ReportFormat reportFormat;

    private String description;

    private Map<String, String> parameters;

    private LocalDateTime scheduledAt;

    // --- Getters and Setters ---

    public String getReportName() { return reportName; }
    public void setReportName(String reportName) { this.reportName = reportName; }

    public ReportType getReportType() { return reportType; }
    public void setReportType(ReportType reportType) { this.reportType = reportType; }

    public ReportFormat getReportFormat() { return reportFormat; }
    public void setReportFormat(ReportFormat reportFormat) { this.reportFormat = reportFormat; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Map<String, String> getParameters() { return parameters; }
    public void setParameters(Map<String, String> parameters) { this.parameters = parameters; }

    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(LocalDateTime scheduledAt) { this.scheduledAt = scheduledAt; }
}
