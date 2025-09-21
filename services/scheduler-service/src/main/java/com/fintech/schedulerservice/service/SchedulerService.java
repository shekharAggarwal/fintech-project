package com.fintech.schedulerservice.service;

import com.fintech.schedulerservice.dto.JobRequest;
import com.fintech.schedulerservice.dto.JobResponse;
import com.fintech.schedulerservice.dto.JobStatusUpdate;
import com.fintech.schedulerservice.model.JobStatus;
import com.fintech.schedulerservice.model.JobType;
import com.fintech.schedulerservice.model.ScheduledJob;
import com.fintech.schedulerservice.repository.ScheduledJobRepository;
import com.fintech.schedulerservice.util.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for managing scheduled jobs
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerService {

    private final ScheduledJobRepository scheduledJobRepository;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final Scheduler quartzScheduler;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Create a new scheduled job
     */
    @Transactional
    public JobResponse createJob(JobRequest jobRequest) {
        log.info("Creating new job: {}", jobRequest.getJobName());

        String jobId = snowflakeIdGenerator.nextId();
        
        ScheduledJob scheduledJob = ScheduledJob.builder()
                .jobId(jobId)
                .jobName(jobRequest.getJobName())
                .jobType(jobRequest.getJobType())
                .jobStatus(JobStatus.SCHEDULED)
                .scheduledTime(jobRequest.getScheduledTime())
                .description(jobRequest.getDescription())
                .createdBy(jobRequest.getCreatedBy())
                .lastUpdatedBy(jobRequest.getCreatedBy())
                .jobData(jobRequest.getJobData())
                .retryCount(0)
                .maxRetries(jobRequest.getMaxRetries() != null ? jobRequest.getMaxRetries() : 3)
                .retryDelaySeconds(jobRequest.getRetryDelaySeconds() != null ? jobRequest.getRetryDelaySeconds() : 60)
                .priority(jobRequest.getPriority() != null ? jobRequest.getPriority() : "NORMAL")
                .build();

        scheduledJob = scheduledJobRepository.save(scheduledJob);

        // Schedule with Quartz
        scheduleWithQuartz(scheduledJob);

        // Publish job created event
        publishJobEvent("job.created", scheduledJob);

        log.info("Job created successfully: {}", jobId);
        return convertToResponse(scheduledJob);
    }

    /**
     * Get job by ID
     */
    public Optional<JobResponse> getJobById(String jobId) {
        return scheduledJobRepository.findById(jobId)
                .map(this::convertToResponse);
    }

    /**
     * Get jobs by status
     */
    public Page<JobResponse> getJobsByStatus(JobStatus jobStatus, Pageable pageable) {
        Page<ScheduledJob> jobsPage = scheduledJobRepository.findByJobStatus(jobStatus, pageable);
        List<JobResponse> responses = jobsPage.getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return new PageImpl<>(responses, pageable, jobsPage.getTotalElements());
    }

    /**
     * Get jobs by type
     */
    public List<JobResponse> getJobsByType(JobType jobType) {
        return scheduledJobRepository.findByJobType(jobType).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update job status
     */
    @Transactional
    public JobResponse updateJobStatus(JobStatusUpdate statusUpdate) {
        log.info("Updating job status: {} -> {}", statusUpdate.getJobId(), statusUpdate.getJobStatus());

        ScheduledJob job = scheduledJobRepository.findById(statusUpdate.getJobId())
                .orElseThrow(() -> new RuntimeException("Job not found: " + statusUpdate.getJobId()));

        job.setJobStatus(statusUpdate.getJobStatus());
        job.setExecutionResult(statusUpdate.getExecutionResult());
        job.setErrorMessage(statusUpdate.getErrorMessage());
        job.setLastUpdatedBy(statusUpdate.getUpdatedBy());
        job.setUpdatedAt(LocalDateTime.now());

        if (statusUpdate.getExecutionTime() != null) {
            job.setActualExecutionTime(statusUpdate.getExecutionTime());
        }

        // Handle status-specific logic
        if (statusUpdate.getJobStatus() == JobStatus.IN_PROGRESS) {
            job.setActualExecutionTime(LocalDateTime.now());
        } else if (statusUpdate.getJobStatus() == JobStatus.FAILED) {
            job.setRetryCount(job.getRetryCount() + 1);
            
            // Schedule retry if within retry limits
            if (job.getRetryCount() < job.getMaxRetries()) {
                LocalDateTime nextRetryTime = LocalDateTime.now().plusSeconds(job.getRetryDelaySeconds());
                job.setScheduledTime(nextRetryTime);
                job.setJobStatus(JobStatus.SCHEDULED);
                scheduleWithQuartz(job);
                log.info("Job scheduled for retry {} of {}: {}", 
                    job.getRetryCount(), job.getMaxRetries(), job.getJobId());
            }
        }

        job = scheduledJobRepository.save(job);

        // Publish status update event
        publishJobEvent("job.status.updated", job);

        log.info("Job status updated successfully: {}", statusUpdate.getJobId());
        return convertToResponse(job);
    }

    /**
     * Cancel a scheduled job
     */
    @Transactional
    public JobResponse cancelJob(String jobId, String updatedBy) {
        log.info("Cancelling job: {}", jobId);

        ScheduledJob job = scheduledJobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

        if (job.getJobStatus() == JobStatus.COMPLETED || job.getJobStatus() == JobStatus.CANCELLED) {
            throw new RuntimeException("Job cannot be cancelled in current status: " + job.getJobStatus());
        }

        job.setJobStatus(JobStatus.CANCELLED);
        job.setLastUpdatedBy(updatedBy);
        job.setUpdatedAt(LocalDateTime.now());

        job = scheduledJobRepository.save(job);

        // Remove from Quartz scheduler
        try {
            JobKey jobKey = new JobKey(jobId, "DEFAULT");
            quartzScheduler.deleteJob(jobKey);
        } catch (SchedulerException e) {
            log.warn("Failed to remove job from Quartz scheduler: {}", jobId, e);
        }

        // Publish job cancelled event
        publishJobEvent("job.cancelled", job);

        log.info("Job cancelled successfully: {}", jobId);
        return convertToResponse(job);
    }

    /**
     * Get jobs ready for execution
     */
    public List<JobResponse> getJobsReadyForExecution() {
        return scheduledJobRepository.findJobsReadyForExecution(LocalDateTime.now()).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get jobs for retry
     */
    public List<JobResponse> getJobsForRetry() {
        return scheduledJobRepository.findJobsForRetry().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Clean up old completed jobs
     */
    @Transactional
    public void cleanupOldJobs(int daysOld) {
        log.info("Cleaning up jobs older than {} days", daysOld);
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        scheduledJobRepository.deleteByJobStatusAndUpdatedAtBefore(JobStatus.COMPLETED, cutoffDate);
        log.info("Old jobs cleanup completed");
    }

    /**
     * Schedule job with Quartz
     */
    private void scheduleWithQuartz(ScheduledJob scheduledJob) {
        try {
            JobDetail jobDetail = JobBuilder.newJob(QuartzJobService.class)
                    .withIdentity(scheduledJob.getJobId(), "DEFAULT")
                    .usingJobData("jobId", scheduledJob.getJobId())
                    .build();

            Date triggerDate = Date.from(scheduledJob.getScheduledTime()
                    .atZone(ZoneId.systemDefault()).toInstant());

            Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(scheduledJob.getJobId() + "_trigger", "DEFAULT")
                    .startAt(triggerDate)
                    .build();

            quartzScheduler.scheduleJob(jobDetail, trigger);
            log.info("Job scheduled with Quartz: {}", scheduledJob.getJobId());
        } catch (SchedulerException e) {
            log.error("Failed to schedule job with Quartz: {}", scheduledJob.getJobId(), e);
            throw new RuntimeException("Failed to schedule job", e);
        }
    }

    /**
     * Publish job event to Kafka
     */
    private void publishJobEvent(String eventType, ScheduledJob job) {
        try {
            kafkaTemplate.send("scheduler-events", eventType, convertToResponse(job));
            log.debug("Published event: {} for job: {}", eventType, job.getJobId());
        } catch (Exception e) {
            log.error("Failed to publish event: {} for job: {}", eventType, job.getJobId(), e);
        }
    }

    /**
     * Convert entity to response DTO
     */
    private JobResponse convertToResponse(ScheduledJob job) {
        return JobResponse.builder()
                .jobId(job.getJobId())
                .jobName(job.getJobName())
                .jobType(job.getJobType())
                .jobStatus(job.getJobStatus())
                .scheduledTime(job.getScheduledTime())
                .actualExecutionTime(job.getActualExecutionTime())
                .description(job.getDescription())
                .createdBy(job.getCreatedBy())
                .lastUpdatedBy(job.getLastUpdatedBy())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .jobData(job.getJobData())
                .executionResult(job.getExecutionResult())
                .errorMessage(job.getErrorMessage())
                .retryCount(job.getRetryCount())
                .maxRetries(job.getMaxRetries())
                .retryDelaySeconds(job.getRetryDelaySeconds())
                .priority(job.getPriority())
                .build();
    }
}