package com.fintech.paymentservice.saga;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "saga_states", indexes = {
    @Index(name = "idx_saga_payment_id", columnList = "payment_id"),
    @Index(name = "idx_saga_status", columnList = "status")
})
public class SagaState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payment_id", nullable = false, unique = true)
    private String paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SagaStatus status = SagaStatus.IN_PROGRESS;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_step")
    private SagaStep currentStep;

    @Column(name = "completed_steps", columnDefinition = "TEXT")
    private String completedSteps; // JSON array of completed steps

    @Column(name = "compensation_data", columnDefinition = "TEXT")
    private String compensationData; // JSON object with data needed for compensation

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    public SagaState() {}

    public SagaState(String paymentId) {
        this.paymentId = paymentId;
        this.status = SagaStatus.IN_PROGRESS;
        this.startedAt = Instant.now();
        this.completedSteps = "[]";
        this.compensationData = "{}";
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public SagaStatus getStatus() { return status; }
    public void setStatus(SagaStatus status) { this.status = status; }

    public SagaStep getCurrentStep() { return currentStep; }
    public void setCurrentStep(SagaStep currentStep) { this.currentStep = currentStep; }

    public String getCompletedSteps() { return completedSteps; }
    public void setCompletedSteps(String completedSteps) { this.completedSteps = completedSteps; }

    public String getCompensationData() { return compensationData; }
    public void setCompensationData(String compensationData) { this.compensationData = compensationData; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public Long getVersion() { return version; }
}
