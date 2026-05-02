package com.fintech.reportingservice.repository;

import com.fintech.reportingservice.entity.Report;
import com.fintech.reportingservice.model.ReportStatus;
import com.fintech.reportingservice.model.ReportType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, String> {

    Page<Report> findByReportType(ReportType reportType, Pageable pageable);

    Page<Report> findByReportStatus(ReportStatus reportStatus, Pageable pageable);

    Page<Report> findByReportTypeAndReportStatus(ReportType reportType, ReportStatus reportStatus, Pageable pageable);

    Page<Report> findByCreatedBy(String createdBy, Pageable pageable);

    @Query("SELECT r FROM Report r WHERE " +
           "(:type IS NULL OR r.reportType = :type) AND " +
           "(:status IS NULL OR r.reportStatus = :status) AND " +
           "(:createdBy IS NULL OR r.createdBy = :createdBy)")
    Page<Report> findByFilters(
            @Param("type") ReportType type,
            @Param("status") ReportStatus status,
            @Param("createdBy") String createdBy,
            Pageable pageable);

    List<Report> findByReportStatusAndExpiresAtBefore(ReportStatus status, LocalDateTime dateTime);

    @Query("SELECT r FROM Report r WHERE r.reportStatus = 'IN_PROGRESS' AND r.startedAt < :timeout")
    List<Report> findStaleInProgressReports(@Param("timeout") LocalDateTime timeout);
}
