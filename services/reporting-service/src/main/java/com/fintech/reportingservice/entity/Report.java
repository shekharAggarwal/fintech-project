package com.fintech.reportingservice.entity;

import com.fintech.reportingservice.model.ReportFormat;
import com.fintech.reportingservice.model.ReportStatus;
import com.fintech.reportingservice.model.ReportType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entity for reports
 */
@Entity
@Table(name = "reports", indexes = {
    @Index(name = "idx_report_type", columnList = "report_type"),
    @Index(name = "idx_report_status", columnList = "report_status"),
    @Index(name = "idx_created_by", columnList = "created_by"),
    @Index(name = "idx_created_at", columnList = "created_at"),
    @Index(name = "idx_status_created", columnList = "report_status, created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Report {

    @Id
    @Column(name = "report_id", length = 50)
    private String reportId;

    @Column(name = "report_name", nullable = false, length = 200)
    private String reportName;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 50)
    private ReportType reportType;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_status", nullable = false, length = 30)
    private ReportStatus reportStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_format", nullable = false, length = 20)
    private ReportFormat reportFormat;

    @Column(name = "description", length = 1000)
    private String description;

    @ElementCollection
    @CollectionTable(name = "report_parameters", joinColumns = @JoinColumn(name = "report_id"))
    @MapKeyColumn(name = "param_key")
    @Column(name = "param_value", length = 1000)
    private Map<String, String> parameters;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "record_count")
    private Long recordCount;

    @Column(name = "generation_time_ms")
    private Long generationTimeMs;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "download_count")
    private Integer downloadCount = 0;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Check if report is expired
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Check if report is ready for download
     */
    public boolean isReadyForDownload() {
        return reportStatus == ReportStatus.COMPLETED && 
               filePath != null && 
               !isExpired();
    }
}