package com.fintech.reportingservice.dto.response;

/**
 * Response DTO for report download containing file metadata and content location.
 */
public class ReportDownloadResponse {

    private String reportId;
    private String reportName;
    private String fileName;
    private String contentType;
    private Long fileSizeBytes;
    private String downloadUrl;
    private byte[] content;

    // --- Getters and Setters ---

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public String getReportName() { return reportName; }
    public void setReportName(String reportName) { this.reportName = reportName; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

    public byte[] getContent() { return content; }
    public void setContent(byte[] content) { this.content = content; }
}
