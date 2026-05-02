package com.fintech.ledgerservice.audit.repository;

import com.fintech.ledgerservice.audit.entity.AuditEvent;
import com.fintech.ledgerservice.audit.entity.AuditEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface AuditRepository extends JpaRepository<AuditEvent, String>, JpaSpecificationExecutor<AuditEvent> {

    Page<AuditEvent> findByActorId(String actorId, Pageable pageable);

    Page<AuditEvent> findByEventType(AuditEventType eventType, Pageable pageable);

    Page<AuditEvent> findByResourceId(String resourceId, Pageable pageable);

    @Query("SELECT ae FROM AuditEvent ae WHERE ae.timestamp BETWEEN :startDate AND :endDate ORDER BY ae.timestamp DESC")
    Page<AuditEvent> findByDateRange(
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable);

    @Query("SELECT ae FROM AuditEvent ae WHERE ae.actorId = :actorId AND ae.timestamp BETWEEN :startDate AND :endDate ORDER BY ae.timestamp DESC")
    Page<AuditEvent> findByActorIdAndDateRange(
            @Param("actorId") String actorId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate,
            Pageable pageable);
}
