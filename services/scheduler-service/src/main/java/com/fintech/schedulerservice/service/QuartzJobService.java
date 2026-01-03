package com.fintech.schedulerservice.service;

import com.fintech.schedulerservice.entity.JobStatus;
import com.fintech.schedulerservice.entity.ScheduledJob;
import com.fintech.schedulerservice.repository.ScheduledJobRepository;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Quartz job service for executing scheduled jobs
 */
@Service
public class QuartzJobService implements Job {

    private static final Logger log = LoggerFactory.getLogger(QuartzJobService.class);

    private final ScheduledJobRepository scheduledJobRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public QuartzJobService(ScheduledJobRepository scheduledJobRepository, KafkaTemplate<String, Object> kafkaTemplate) {
        this.scheduledJobRepository = scheduledJobRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    @Transactional
    public void execute(JobExecutionContext context) throws JobExecutionException {
        String jobId = context.getJobDetail().getJobDataMap().getString("jobId");
        log.info("Executing scheduled job: {}", jobId);

        Optional<ScheduledJob> optionalJob = scheduledJobRepository.findById(jobId);
        if (!optionalJob.isPresent()) {
            log.error("Job not found: {}", jobId);
            return;
        }

        ScheduledJob scheduledJob = optionalJob.get();
        
        try {
            // Update job status to IN_PROGRESS
            scheduledJob.setJobStatus(JobStatus.IN_PROGRESS);
            scheduledJob.setActualExecutionTime(LocalDateTime.now());
            scheduledJob.setUpdatedAt(Instant.now());
            scheduledJobRepository.save(scheduledJob);

            // Publish job started event
            publishJobEvent("job.execution.started", scheduledJob);

            // Execute job based on type
            executeJobByType(scheduledJob);

            // Update job status to COMPLETED
            scheduledJob.setJobStatus(JobStatus.COMPLETED);
            scheduledJob.setExecutionResult("Job completed successfully");
            scheduledJob.setUpdatedAt(Instant.now());
            scheduledJobRepository.save(scheduledJob);

            // Publish job completed event
            publishJobEvent("job.execution.completed", scheduledJob);

            log.info("Job executed successfully: {}", jobId);

        } catch (Exception e) {
            log.error("Job execution failed: {}", jobId, e);

            // Update job status to FAILED
            scheduledJob.setJobStatus(JobStatus.FAILED);
            scheduledJob.setErrorMessage(e.getMessage());
            scheduledJob.setRetryCount(scheduledJob.getRetryCount() + 1);
            scheduledJob.setUpdatedAt(Instant.now());
            scheduledJobRepository.save(scheduledJob);

            // Publish job failed event
            publishJobEvent("job.execution.failed", scheduledJob);

            throw new JobExecutionException(e);
        }
    }

    /**
     * Execute job based on its type
     */
    private void executeJobByType(ScheduledJob job) {
        log.info("Executing job type: {} for job: {}", job.getJobType(), job.getJobId());

        switch (job.getJobType()) {
            case PAYMENT_RETRY:
                executePaymentRetryJob(job);
                break;
            case TRANSACTION_RETRY:
                executeTransactionRetryJob(job);
                break;
            case NOTIFICATION_REMINDER:
                executeNotificationReminderJob(job);
                break;
            case ACCOUNT_CLEANUP:
                executeAccountCleanupJob(job);
                break;
            case REPORT_GENERATION:
                executeReportGenerationJob(job);
                break;
            case DATA_SYNC:
                executeDataSyncJob(job);
                break;
            case BATCH_PROCESSING:
                executeBatchProcessingJob(job);
                break;
            default:
                log.warn("Unknown job type: {} for job: {}", job.getJobType(), job.getJobId());
                throw new RuntimeException("Unknown job type: " + job.getJobType());
        }
    }

    /**
     * Execute payment retry job
     */
    private void executePaymentRetryJob(ScheduledJob job) {
        log.info("Executing payment retry job: {}", job.getJobId());
        
        // Extract payment data from job data
        String jobDataStr = job.getJobData();
        log.info("Job data for payment retry: {}", jobDataStr);
        
        // Publish payment retry event
        kafkaTemplate.send("payment-events", "payment.retry.requested", jobDataStr);
        
        log.info("Payment retry event published");
    }

    /**
     * Execute transaction retry job
     */
    private void executeTransactionRetryJob(ScheduledJob job) {
        log.info("Executing transaction retry job: {}", job.getJobId());
        
        // Extract transaction data from job data
        String jobDataStr = job.getJobData();
        log.info("Job data for transaction retry: {}", jobDataStr);
        
        // Publish transaction retry event
        kafkaTemplate.send("transaction-events", "transaction.retry.requested", jobDataStr);
        
        log.info("Transaction retry event published");
    }

    /**
     * Execute notification reminder job
     */
    private void executeNotificationReminderJob(ScheduledJob job) {
        log.info("Executing notification reminder job: {}", job.getJobId());
        
        // Extract notification data from job data
        String jobDataStr = job.getJobData();
        log.info("Job data for notification reminder: {}", jobDataStr);
        
        // Publish notification reminder event
        kafkaTemplate.send("notification-events", "notification.reminder.requested", jobDataStr);
        
        log.info("Notification reminder event published");
    }

    /**
     * Execute account cleanup job
     */
    private void executeAccountCleanupJob(ScheduledJob job) {
        log.info("Executing account cleanup job: {}", job.getJobId());
        
        // Publish account cleanup event
        kafkaTemplate.send("user-events", "account.cleanup.requested", job.getJobData());
        
        log.info("Account cleanup event published");
    }

    /**
     * Execute report generation job
     */
    private void executeReportGenerationJob(ScheduledJob job) {
        log.info("Executing report generation job: {}", job.getJobId());
        
        // Extract report data from job data
        String jobDataStr = job.getJobData();
        log.info("Job data for report generation: {}", jobDataStr);
        
        // Publish report generation event
        kafkaTemplate.send("reporting-events", "report.generation.requested", jobDataStr);
        
        log.info("Report generation event published");
    }

    /**
     * Execute data sync job
     */
    private void executeDataSyncJob(ScheduledJob job) {
        log.info("Executing data sync job: {}", job.getJobId());
        
        // Extract sync data from job data
        String jobDataStr = job.getJobData();
        log.info("Job data for data sync: {}", jobDataStr);
        
        // Publish data sync event
        kafkaTemplate.send("ledger-events", "data.sync.requested", jobDataStr);
        
        log.info("Data sync event published");
    }

    /**
     * Execute batch processing job
     */
    private void executeBatchProcessingJob(ScheduledJob job) {
        log.info("Executing batch processing job: {}", job.getJobId());
        
        // Extract batch data from job data
        String jobDataStr = job.getJobData();
        log.info("Job data for batch processing: {}", jobDataStr);
        
        // Publish batch processing event
        kafkaTemplate.send("batch-events", "batch.processing.requested", jobDataStr);
        
        log.info("Batch processing event published");
    }

    /**
     * Publish job event to Kafka
     */
    private void publishJobEvent(String eventType, ScheduledJob job) {
        try {
            kafkaTemplate.send("scheduler-events", eventType, job);
            log.debug("Published event: {} for job: {}", eventType, job.getJobId());
        } catch (Exception e) {
            log.error("Failed to publish event: {} for job: {}", eventType, job.getJobId(), e);
        }
    }
}