package com.fintech.reportingservice.exception;

/**
 * General exception for report service operations.
 */
public class ReportServiceException extends RuntimeException {

    private final String errorCode;

    public ReportServiceException(String message) {
        super(message);
        this.errorCode = "REPORT_SERVICE_ERROR";
    }

    public ReportServiceException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public ReportServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "REPORT_SERVICE_ERROR";
    }

    public ReportServiceException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
