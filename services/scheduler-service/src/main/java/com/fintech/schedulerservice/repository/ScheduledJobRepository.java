package com.fintech.schedulerservice.repository;

import com.fintech.schedulerservice.model.JobStatus;
import com.fintech.schedulerservice.model.JobType;
import com.fintech.schedulerservice.model.ScheduledJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ScheduledJob entity
 */
@Repository
public interface ScheduledJobRepository extends JpaRepository<ScheduledJob, String> {

    /**
     * Find jobs by status
     */
    List<ScheduledJob> findByJobStatus(JobStatus jobStatus);

    /**
     * Find jobs by status with pagination
     */
    Page<ScheduledJob> findByJobStatus(JobStatus jobStatus, Pageable pageable);

    /**
     * Find jobs by type
     */
    List<ScheduledJob> findByJobType(JobType jobType);

    /**
     * Find jobs by type and status
     */
    List<ScheduledJob> findByJobTypeAndJobStatus(JobType jobType, JobStatus jobStatus);

    /**
     * Find jobs scheduled before a specific time
     */
    List<ScheduledJob> findByScheduledTimeBefore(LocalDateTime dateTime);

    /**
     * Find jobs ready for execution (scheduled and scheduled time has passed)
     */
    @Query("SELECT j FROM ScheduledJob j WHERE j.jobStatus = 'SCHEDULED' AND j.scheduledTime <= :currentTime")
    List<ScheduledJob> findJobsReadyForExecution(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Find jobs that need retry (failed status and retry count < max retries)
     */
    @Query("SELECT j FROM ScheduledJob j WHERE j.jobStatus = 'FAILED' AND j.retryCount < j.maxRetries")
    List<ScheduledJob> findJobsForRetry();

    /**
     * Find jobs by created by user
     */
    List<ScheduledJob> findByCreatedByOrderByCreatedAtDesc(String createdBy);

    /**
     * Find jobs created between dates
     */
    List<ScheduledJob> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Find jobs by job name pattern
     */
    List<ScheduledJob> findByJobNameContainingIgnoreCase(String jobNamePattern);

    /**
     * Count jobs by status
     */
    long countByJobStatus(JobStatus jobStatus);

    /**
     * Count jobs by type and status
     */
    long countByJobTypeAndJobStatus(JobType jobType, JobStatus jobStatus);

    /**
     * Find all jobs ordered by scheduled time
     */
    List<ScheduledJob> findAllByOrderByScheduledTimeAsc();

    /**
     * Find jobs with high retry count (potential problematic jobs)
     */
    @Query("SELECT j FROM ScheduledJob j WHERE j.retryCount >= :minRetryCount ORDER BY j.retryCount DESC")
    List<ScheduledJob> findJobsWithHighRetryCount(@Param("minRetryCount") Integer minRetryCount);

    /**
     * Find stuck jobs (in progress for too long)
     */
    @Query("SELECT j FROM ScheduledJob j WHERE j.jobStatus = 'IN_PROGRESS' AND j.updatedAt < :cutoffTime")
    List<ScheduledJob> findStuckJobs(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Delete completed jobs older than specified date
     */
    void deleteByJobStatusAndUpdatedAtBefore(JobStatus jobStatus, LocalDateTime cutoffDate);
}