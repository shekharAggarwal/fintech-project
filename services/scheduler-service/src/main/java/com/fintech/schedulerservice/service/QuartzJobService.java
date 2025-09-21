package com.fintech.schedulerservice.service;

import com.fintech.schedulerservice.model.JobStatus;
import com.fintech.schedulerservice.model.ScheduledJob;
import com.fintech.schedulerservice.repository.ScheduledJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Quartz job service for executing scheduled jobs
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuartzJobService implements Job {

    private final ScheduledJobRepository scheduledJobRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

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
            scheduledJob.setUpdatedAt(LocalDateTime.now());
            scheduledJobRepository.save(scheduledJob);

            // Publish job started event
            publishJobEvent("job.execution.started", scheduledJob);

            // Execute job based on type
            executeJobByType(scheduledJob);

            // Update job status to COMPLETED
            scheduledJob.setJobStatus(JobStatus.COMPLETED);
            scheduledJob.setExecutionResult("Job completed successfully");
            scheduledJob.setUpdatedAt(LocalDateTime.now());
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
            scheduledJob.setUpdatedAt(LocalDateTime.now());
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
        String paymentId = (String) job.getJobData().get("paymentId");
        
        // Publish payment retry event
        kafkaTemplate.send("payment-events", "payment.retry.requested", 
            job.getJobData());
        
        log.info("Payment retry event published for payment: {}", paymentId);
    }

    /**
     * Execute transaction retry job
     */
    private void executeTransactionRetryJob(ScheduledJob job) {
        log.info("Executing transaction retry job: {}", job.getJobId());
        
        // Extract transaction data from job data
        String transactionId = (String) job.getJobData().get("transactionId");
        
        // Publish transaction retry event
        kafkaTemplate.send("transaction-events", "transaction.retry.requested", 
            job.getJobData());
        
        log.info("Transaction retry event published for transaction: {}", transactionId);
    }

    /**
     * Execute notification reminder job
     */
    private void executeNotificationReminderJob(ScheduledJob job) {
        log.info("Executing notification reminder job: {}", job.getJobId());
        
        // Extract notification data from job data
        String userId = (String) job.getJobData().get("userId");
        String notificationType = (String) job.getJobData().get("notificationType");
        
        // Publish notification reminder event
        kafkaTemplate.send("notification-events", "notification.reminder.requested", 
            job.getJobData());
        
        log.info("Notification reminder event published for user: {} type: {}", userId, notificationType);
    }

    /**
     * Execute account cleanup job
     */
    private void executeAccountCleanupJob(ScheduledJob job) {
        log.info("Executing account cleanup job: {}", job.getJobId());
        
        // Publish account cleanup event
        kafkaTemplate.send("user-events", "account.cleanup.requested", 
            job.getJobData());
        
        log.info("Account cleanup event published");
    }

    /**
     * Execute report generation job
     */
    private void executeReportGenerationJob(ScheduledJob job) {
        log.info("Executing report generation job: {}", job.getJobId());
        
        // Extract report data from job data
        String reportType = (String) job.getJobData().get("reportType");
        
        // Publish report generation event
        kafkaTemplate.send("reporting-events", "report.generation.requested", 
            job.getJobData());
        
        log.info("Report generation event published for type: {}", reportType);
    }

    /**
     * Execute data sync job
     */
    private void executeDataSyncJob(ScheduledJob job) {
        log.info("Executing data sync job: {}", job.getJobId());
        
        // Extract sync data from job data
        String syncType = (String) job.getJobData().get("syncType");
        
        // Publish data sync event
        kafkaTemplate.send("ledger-events", "data.sync.requested", 
            job.getJobData());
        
        log.info("Data sync event published for type: {}", syncType);
    }

    /**
     * Execute batch processing job
     */
    private void executeBatchProcessingJob(ScheduledJob job) {
        log.info("Executing batch processing job: {}", job.getJobId());
        
        // Extract batch data from job data
        String batchType = (String) job.getJobData().get("batchType");
        
        // Publish batch processing event
        kafkaTemplate.send("batch-events", "batch.processing.requested", 
            job.getJobData());
        
        log.info("Batch processing event published for type: {}", batchType);
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