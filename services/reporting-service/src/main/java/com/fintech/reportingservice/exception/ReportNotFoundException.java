package com.fintech.reportingservice.exception;

/**
 * Exception thrown when a requested report cannot be found.
 */
public class ReportNotFoundException extends RuntimeException {

    private final String reportId;

    public ReportNotFoundException(String reportId) {
        super("Report not found with id: " + reportId);
        this.reportId = reportId;
    }

    public ReportNotFoundException(String reportId, String message) {
        super(message);
        this.reportId = reportId;
    }

    public String getReportId() {
        return reportId;
    }
}
