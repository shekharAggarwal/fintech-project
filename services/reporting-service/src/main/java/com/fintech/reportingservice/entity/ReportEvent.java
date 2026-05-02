package com.fintech.reportingservice.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Event store entity for Kafka events consumed by the reporting service.
 * Stores raw event data for report generation and audit trails.
 */
@Entity
@Table(name = "report_events", indexes = {
    @Index(name = "idx_event_type", columnList = "event_type"),
    @Index(name = "idx_source_service", columnList = "source_service"),
    @Index(name = "idx_received_at", columnList = "received_at"),
    @Index(name = "idx_event_type_received", columnList = "event_type, received_at"),
    @Index(name = "idx_account_id", columnList = "account_id")
})
public class ReportEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "source_service", nullable = false, length = 100)
    private String sourceService;

    @Column(name = "account_id", length = 100)
    private String accountId;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @CreationTimestamp
    @Column(name = "received_at", nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public String getSourceService() { return sourceService; }
    public void setSourceService(String sourceService) { this.sourceService = sourceService; }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }

    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }
}
