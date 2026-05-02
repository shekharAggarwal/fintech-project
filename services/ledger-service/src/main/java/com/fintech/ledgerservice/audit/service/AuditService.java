package com.fintech.ledgerservice.audit.service;

import com.fintech.ledgerservice.audit.dto.AuditSearchCriteria;
import com.fintech.ledgerservice.audit.entity.*;
import com.fintech.ledgerservice.audit.repository.AuditRepository;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    private final AuditRepository auditRepository;

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    /**
     * Records a new audit event. This is the primary entry point for audit logging.
     */
    public AuditEvent recordEvent(AuditEventType eventType, String actorId, ActorType actorType,
                                  ResourceType resourceType, String resourceId, AuditAction action,
                                  String details, String ipAddress) {
        logger.debug("Recording audit event: type={}, actor={}, resource={}/{}",
                eventType, actorId, resourceType, resourceId);

        AuditEvent event = new AuditEvent(eventType, actorId, actorType, resourceType, resourceId, action, details, ipAddress);
        AuditEvent saved = auditRepository.save(event);

        logger.info("Audit event recorded: id={}, type={}, actor={}", saved.getId(), eventType, actorId);
        return saved;
    }

    /**
     * Get the full audit trail with pagination, ordered by timestamp descending.
     */
    public Page<AuditEvent> getAuditTrail(Pageable pageable) {
        return auditRepository.findAll(pageable);
    }

    /**
     * Get audit events by actor ID.
     */
    public Page<AuditEvent> getAuditByActor(String actorId, Pageable pageable) {
        return auditRepository.findByActorId(actorId, pageable);
    }

    /**
     * Get audit events by event type.
     */
    public Page<AuditEvent> getAuditByType(AuditEventType eventType, Pageable pageable) {
        return auditRepository.findByEventType(eventType, pageable);
    }

    /**
     * Get audit events by date range.
     */
    public Page<AuditEvent> getAuditByDateRange(Instant startDate, Instant endDate, Pageable pageable) {
        return auditRepository.findByDateRange(startDate, endDate, pageable);
    }

    /**
     * Search audit events using flexible criteria with JPA Specifications.
     */
    public Page<AuditEvent> searchAudit(AuditSearchCriteria criteria, Pageable pageable) {
        Specification<AuditEvent> spec = buildSpecification(criteria);
        return auditRepository.findAll(spec, pageable);
    }

    private Specification<AuditEvent> buildSpecification(AuditSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getActorId() != null) {
                predicates.add(cb.equal(root.get("actorId"), criteria.getActorId()));
            }
            if (criteria.getActorType() != null) {
                predicates.add(cb.equal(root.get("actorType"), criteria.getActorType()));
            }
            if (criteria.getEventType() != null) {
                predicates.add(cb.equal(root.get("eventType"), criteria.getEventType()));
            }
            if (criteria.getResourceType() != null) {
                predicates.add(cb.equal(root.get("resourceType"), criteria.getResourceType()));
            }
            if (criteria.getResourceId() != null) {
                predicates.add(cb.equal(root.get("resourceId"), criteria.getResourceId()));
            }
            if (criteria.getAction() != null) {
                predicates.add(cb.equal(root.get("action"), criteria.getAction()));
            }
            if (criteria.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), criteria.getStartDate()));
            }
            if (criteria.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), criteria.getEndDate()));
            }

            query.orderBy(cb.desc(root.get("timestamp")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
