package com.fintech.ledgerservice.audit.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable audit event entity.
 * Once created, audit records cannot be modified or deleted.
 */
@Entity
@Table(name = "audit_events", indexes = {
        @Index(name = "idx_audit_actor_id", columnList = "actor_id"),
        @Index(name = "idx_audit_event_type", columnList = "event_type"),
        @Index(name = "idx_audit_resource", columnList = "resource_type, resource_id"),
        @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
        @Index(name = "idx_audit_action", columnList = "action")
})
public class AuditEvent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private final String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, updatable = false, length = 50)
    private final AuditEventType eventType;

    @Column(name = "actor_id", nullable = false, updatable = false, length = 100)
    private final String actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, updatable = false, length = 20)
    private final ActorType actorType;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, updatable = false, length = 30)
    private final ResourceType resourceType;

    @Column(name = "resource_id", nullable = false, updatable = false, length = 100)
    private final String resourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, updatable = false, length = 10)
    private final AuditAction action;

    @Column(name = "details", updatable = false, columnDefinition = "TEXT")
    private final String details;

    @Column(name = "ip_address", updatable = false, length = 45)
    private final String ipAddress;

    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant timestamp;

    // Prevent updates and deletes — immutable audit trail
    @PreUpdate
    protected void onUpdate() {
        throw new UnsupportedOperationException("Audit events are immutable and cannot be updated.");
    }

    @PreRemove
    protected void onRemove() {
        throw new UnsupportedOperationException("Audit events are immutable and cannot be deleted.");
    }

    // Protected no-arg constructor for JPA (spec allows protected)
    protected AuditEvent() {
        this.id = UUID.randomUUID().toString();
        this.eventType = null;
        this.actorId = null;
        this.actorType = null;
        this.resourceType = null;
        this.resourceId = null;
        this.action = null;
        this.details = null;
        this.ipAddress = null;
    }

    public AuditEvent(AuditEventType eventType, String actorId, ActorType actorType,
                      ResourceType resourceType, String resourceId, AuditAction action,
                      String details, String ipAddress) {
        this.id = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.actorId = actorId;
        this.actorType = actorType;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.action = action;
        this.details = details;
        this.ipAddress = ipAddress;
    }

    // Getters only — no setters, entity is immutable after creation
    public String getId() {
        return id;
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public String getActorId() {
        return actorId;
    }

    public ActorType getActorType() {
        return actorType;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public AuditAction getAction() {
        return action;
    }

    public String getDetails() {
        return details;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
