package com.fintech.retryservice.repository;

import com.fintech.retryservice.model.RetryAttempt;
import com.fintech.retryservice.model.RetryStatus;
import com.fintech.retryservice.model.RetryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for retry attempt operations
 */
@Repository
public interface RetryAttemptRepository extends JpaRepository<RetryAttempt, String> {

    /**
     * Find retry attempts by status
     */
    Page<RetryAttempt> findByRetryStatus(RetryStatus retryStatus, Pageable pageable);

    /**
     * Find retry attempts by type
     */
    List<RetryAttempt> findByRetryType(RetryType retryType);

    /**
     * Find retry attempts by original ID
     */
    List<RetryAttempt> findByOriginalId(String originalId);

    /**
     * Find active retry attempt for original ID
     */
    Optional<RetryAttempt> findByOriginalIdAndRetryStatusIn(String originalId, List<RetryStatus> statuses);

    /**
     * Find retry attempts ready for execution
     */
    @Query("SELECT r FROM RetryAttempt r WHERE r.retryStatus = :status " +
           "AND r.nextRetryTime <= :currentTime AND r.retryCount < r.maxRetries " +
           "ORDER BY r.priority DESC, r.nextRetryTime ASC")
    List<RetryAttempt> findRetryAttemptsReadyForExecution(
        @Param("status") RetryStatus status,
        @Param("currentTime") LocalDateTime currentTime);

    /**
     * Find stuck retry attempts
     */
    @Query("SELECT r FROM RetryAttempt r WHERE r.retryStatus = :status " +
           "AND r.lastRetryTime < :thresholdTime")
    List<RetryAttempt> findStuckRetryAttempts(
        @Param("status") RetryStatus status,
        @Param("thresholdTime") LocalDateTime thresholdTime);

    /**
     * Count retry attempts by status
     */
    long countByRetryStatus(RetryStatus retryStatus);

    /**
     * Count retry attempts by type and status
     */
    long countByRetryTypeAndRetryStatus(RetryType retryType, RetryStatus retryStatus);

    /**
     * Find retry attempts with high retry count
     */
    @Query("SELECT r FROM RetryAttempt r WHERE r.retryCount >= :threshold " +
           "AND r.retryStatus NOT IN (:excludedStatuses)")
    List<RetryAttempt> findHighRetryCountAttempts(
        @Param("threshold") Integer threshold,
        @Param("excludedStatuses") List<RetryStatus> excludedStatuses);

    /**
     * Find retry attempts by service name and status
     */
    List<RetryAttempt> findByServiceNameAndRetryStatus(String serviceName, RetryStatus retryStatus);

    /**
     * Find retry attempts created within time range
     */
    @Query("SELECT r FROM RetryAttempt r WHERE r.createdAt BETWEEN :startTime AND :endTime " +
           "ORDER BY r.createdAt DESC")
    List<RetryAttempt> findRetryAttemptsInTimeRange(
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime);

    /**
     * Delete old completed retry attempts
     */
    @Modifying
    @Query("DELETE FROM RetryAttempt r WHERE r.retryStatus IN (:statuses) " +
           "AND r.updatedAt < :cutoffTime")
    void deleteOldRetryAttempts(
        @Param("statuses") List<RetryStatus> statuses,
        @Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Update retry attempts status
     */
    @Modifying
    @Query("UPDATE RetryAttempt r SET r.retryStatus = :newStatus, r.updatedAt = :updateTime " +
           "WHERE r.retryStatus = :oldStatus AND r.lastRetryTime < :thresholdTime")
    int updateStuckRetryAttempts(
        @Param("oldStatus") RetryStatus oldStatus,
        @Param("newStatus") RetryStatus newStatus,
        @Param("thresholdTime") LocalDateTime thresholdTime,
        @Param("updateTime") LocalDateTime updateTime);

    /**
     * Get retry statistics by type
     */
    @Query("SELECT r.retryType as type, r.retryStatus as status, COUNT(r) as count " +
           "FROM RetryAttempt r GROUP BY r.retryType, r.retryStatus")
    List<Object[]> getRetryStatistics();

    /**
     * Find retry attempts by priority
     */
    @Query("SELECT r FROM RetryAttempt r WHERE r.priority = :priority " +
           "AND r.retryStatus = :status ORDER BY r.nextRetryTime ASC")
    List<RetryAttempt> findByPriorityAndStatus(
        @Param("priority") String priority,
        @Param("status") RetryStatus status);
}