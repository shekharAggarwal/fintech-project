package com.fintech.ledgerservice.audit.dto;

import com.fintech.ledgerservice.audit.entity.ActorType;
import com.fintech.ledgerservice.audit.entity.AuditAction;
import com.fintech.ledgerservice.audit.entity.AuditEventType;
import com.fintech.ledgerservice.audit.entity.ResourceType;

import java.time.Instant;

/**
 * DTO for searching audit events with multiple criteria.
 */
public class AuditSearchCriteria {

    private String actorId;
    private ActorType actorType;
    private AuditEventType eventType;
    private ResourceType resourceType;
    private String resourceId;
    private AuditAction action;
    private Instant startDate;
    private Instant endDate;

    public AuditSearchCriteria() {
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public ActorType getActorType() {
        return actorType;
    }

    public void setActorType(ActorType actorType) {
        this.actorType = actorType;
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public void setEventType(AuditEventType eventType) {
        this.eventType = eventType;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public AuditAction getAction() {
        return action;
    }

    public void setAction(AuditAction action) {
        this.action = action;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }
}
