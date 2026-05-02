package com.fintech.reportingservice.repository;

import com.fintech.reportingservice.entity.ReportEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReportEventRepository extends JpaRepository<ReportEvent, Long> {

    List<ReportEvent> findByEventType(String eventType);

    List<ReportEvent> findBySourceService(String sourceService);

    List<ReportEvent> findByAccountId(String accountId);

    @Query("SELECT e FROM ReportEvent e WHERE e.receivedAt BETWEEN :startDate AND :endDate")
    List<ReportEvent> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT e FROM ReportEvent e WHERE e.eventType = :eventType AND e.receivedAt BETWEEN :startDate AND :endDate")
    List<ReportEvent> findByEventTypeAndDateRange(
            @Param("eventType") String eventType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT e FROM ReportEvent e WHERE e.accountId = :accountId AND e.receivedAt BETWEEN :startDate AND :endDate")
    List<ReportEvent> findByAccountIdAndDateRange(
            @Param("accountId") String accountId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT e FROM ReportEvent e WHERE " +
           "(:eventType IS NULL OR e.eventType = :eventType) AND " +
           "(:accountId IS NULL OR e.accountId = :accountId) AND " +
           "e.receivedAt BETWEEN :startDate AND :endDate")
    Page<ReportEvent> findByFilters(
            @Param("eventType") String eventType,
            @Param("accountId") String accountId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    long countByEventTypeAndReceivedAtBetween(String eventType, LocalDateTime startDate, LocalDateTime endDate);
}
