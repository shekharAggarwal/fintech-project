package com.fintech.schedulerservice.service;

import com.fintech.schedulerservice.dto.*;
import com.fintech.schedulerservice.entity.PaymentFrequency;
import com.fintech.schedulerservice.entity.RecurringPayment;
import com.fintech.schedulerservice.entity.RecurringPaymentStatus;
import com.fintech.schedulerservice.exception.InvalidJobStateException;
import com.fintech.schedulerservice.exception.JobNotFoundException;
import com.fintech.schedulerservice.exception.JobSchedulingException;
import com.fintech.schedulerservice.job.RecurringPaymentJob;
import com.fintech.schedulerservice.repository.RecurringPaymentRepository;
import com.fintech.schedulerservice.util.SnowflakeIdGenerator;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing recurring payments with Quartz scheduling
 */
@Service
public class RecurringPaymentService {

    private static final Logger log = LoggerFactory.getLogger(RecurringPaymentService.class);
    private static final String RECURRING_PAYMENT_GROUP = "RECURRING_PAYMENTS";

    private final RecurringPaymentRepository recurringPaymentRepository;
    private final SnowflakeIdGenerator snowflakeIdGenerator;
    private final Scheduler quartzScheduler;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public RecurringPaymentService(RecurringPaymentRepository recurringPaymentRepository,
                                    SnowflakeIdGenerator snowflakeIdGenerator,
                                    Scheduler quartzScheduler,
                                    KafkaTemplate<String, Object> kafkaTemplate) {
        this.recurringPaymentRepository = recurringPaymentRepository;
        this.snowflakeIdGenerator = snowflakeIdGenerator;
        this.quartzScheduler = quartzScheduler;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Create a new recurring payment and schedule it with Quartz
     */
    @Transactional
    public RecurringPaymentResponse createRecurringPayment(RecurringPaymentRequest request) {
        log.info("Creating recurring payment for user: {}", request.userId());

        String paymentId = snowflakeIdGenerator.nextId();

        RecurringPayment payment = new RecurringPayment();
        payment.setId(paymentId);
        payment.setUserId(request.userId());
        payment.setSourceAccountId(request.sourceAccountId());
        payment.setDestinationAccountId(request.destinationAccountId());
        payment.setAmount(request.amount());
        payment.setCurrency(request.currency());
        payment.setFrequency(request.frequency());
        payment.setStartDate(request.startDate());
        payment.setEndDate(request.endDate());
        payment.setDescription(request.description());
        payment.setMaxRetries(request.maxRetries() != null ? request.maxRetries() : 3);
        payment.setCurrentRetryCount(0);
        payment.setStatus(RecurringPaymentStatus.ACTIVE);
        payment.setNextExecutionDate(request.startDate());

        payment = recurringPaymentRepository.save(payment);

        // Schedule with Quartz
        schedulePaymentWithQuartz(payment);

        // Publish event
        publishPaymentEvent("recurring-payment.created", payment);

        log.info("Recurring payment created: {}", paymentId);
        return convertToResponse(payment);
    }

    /**
     * Get recurring payment by ID
     */
    public RecurringPaymentResponse getPaymentById(String paymentId) {
        RecurringPayment payment = recurringPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new JobNotFoundException(paymentId));
        return convertToResponse(payment);
    }

    /**
     * Get all recurring payments for a user
     */
    public Page<RecurringPaymentResponse> getPaymentsByUserId(String userId, Pageable pageable) {
        Page<RecurringPayment> paymentsPage = recurringPaymentRepository.findByUserId(userId, pageable);
        List<RecurringPaymentResponse> responses = paymentsPage.getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return new PageImpl<>(responses, pageable, paymentsPage.getTotalElements());
    }

    /**
     * Get all recurring payments with pagination
     */
    public Page<RecurringPaymentResponse> getAllPayments(Pageable pageable) {
        Page<RecurringPayment> paymentsPage = recurringPaymentRepository.findAll(pageable);
        List<RecurringPaymentResponse> responses = paymentsPage.getContent().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
        return new PageImpl<>(responses, pageable, paymentsPage.getTotalElements());
    }

    /**
     * Update a recurring payment
     */
    @Transactional
    public RecurringPaymentResponse updatePayment(String paymentId, RecurringPaymentUpdateRequest request) {
        log.info("Updating recurring payment: {}", paymentId);

        RecurringPayment payment = recurringPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new JobNotFoundException(paymentId));

        if (payment.getStatus() == RecurringPaymentStatus.CANCELLED ||
            payment.getStatus() == RecurringPaymentStatus.COMPLETED) {
            throw new InvalidJobStateException(paymentId, null, "update");
        }

        boolean rescheduleRequired = false;

        if (request.amount() != null) {
            payment.setAmount(request.amount());
        }
        if (request.currency() != null) {
            payment.setCurrency(request.currency());
        }
        if (request.frequency() != null && request.frequency() != payment.getFrequency()) {
            payment.setFrequency(request.frequency());
            rescheduleRequired = true;
        }
        if (request.destinationAccountId() != null) {
            payment.setDestinationAccountId(request.destinationAccountId());
        }
        if (request.endDate() != null) {
            payment.setEndDate(request.endDate());
        }
        if (request.description() != null) {
            payment.setDescription(request.description());
        }
        if (request.maxRetries() != null) {
            payment.setMaxRetries(request.maxRetries());
        }

        payment = recurringPaymentRepository.save(payment);

        if (rescheduleRequired && payment.getStatus() == RecurringPaymentStatus.ACTIVE) {
            unschedulePayment(payment);
            schedulePaymentWithQuartz(payment);
        }

        publishPaymentEvent("recurring-payment.updated", payment);

        log.info("Recurring payment updated: {}", paymentId);
        return convertToResponse(payment);
    }

    /**
     * Pause a recurring payment
     */
    @Transactional
    public RecurringPaymentResponse pausePayment(String paymentId) {
        log.info("Pausing recurring payment: {}", paymentId);

        RecurringPayment payment = recurringPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new JobNotFoundException(paymentId));

        if (payment.getStatus() != RecurringPaymentStatus.ACTIVE) {
            throw new InvalidJobStateException(paymentId, null, "pause");
        }

        // Pause in Quartz FIRST to prevent execution during state transition
        try {
            JobKey jobKey = new JobKey(paymentId, RECURRING_PAYMENT_GROUP);
            quartzScheduler.pauseJob(jobKey);
        } catch (SchedulerException e) {
            log.warn("Failed to pause Quartz job for payment: {}", paymentId, e);
        }

        // Then update DB status
        payment.setStatus(RecurringPaymentStatus.PAUSED);
        payment = recurringPaymentRepository.save(payment);

        publishPaymentEvent("recurring-payment.paused", payment);

        log.info("Recurring payment paused: {}", paymentId);
        return convertToResponse(payment);
    }

    /**
     * Resume a paused recurring payment
     */
    @Transactional
    public RecurringPaymentResponse resumePayment(String paymentId) {
        log.info("Resuming recurring payment: {}", paymentId);

        RecurringPayment payment = recurringPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new JobNotFoundException(paymentId));

        if (payment.getStatus() != RecurringPaymentStatus.PAUSED) {
            throw new InvalidJobStateException(paymentId, null, "resume");
        }

        payment.setStatus(RecurringPaymentStatus.ACTIVE);
        payment.setCurrentRetryCount(0);
        payment = recurringPaymentRepository.save(payment);

        // Resume in Quartz
        try {
            JobKey jobKey = new JobKey(paymentId, RECURRING_PAYMENT_GROUP);
            quartzScheduler.resumeJob(jobKey);
        } catch (SchedulerException e) {
            log.warn("Failed to resume Quartz job for payment: {}", paymentId, e);
        }

        publishPaymentEvent("recurring-payment.resumed", payment);

        log.info("Recurring payment resumed: {}", paymentId);
        return convertToResponse(payment);
    }

    /**
     * Cancel a recurring payment
     */
    @Transactional
    public void cancelPayment(String paymentId) {
        log.info("Cancelling recurring payment: {}", paymentId);

        RecurringPayment payment = recurringPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new JobNotFoundException(paymentId));

        if (payment.getStatus() == RecurringPaymentStatus.CANCELLED ||
            payment.getStatus() == RecurringPaymentStatus.COMPLETED) {
            throw new InvalidJobStateException(paymentId, null, "cancel");
        }

        payment.setStatus(RecurringPaymentStatus.CANCELLED);
        recurringPaymentRepository.save(payment);

        // Remove from Quartz
        unschedulePayment(payment);

        publishPaymentEvent("recurring-payment.cancelled", payment);

        log.info("Recurring payment cancelled: {}", paymentId);
    }

    /**
     * Execute a recurring payment (called by Quartz job)
     */
    @Transactional
    public void executePayment(String paymentId) {
        log.info("Executing recurring payment: {}", paymentId);

        RecurringPayment payment = recurringPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new JobNotFoundException(paymentId));

        if (payment.getStatus() != RecurringPaymentStatus.ACTIVE) {
            log.warn("Payment {} is not active, skipping execution. Status: {}", paymentId, payment.getStatus());
            return;
        }

        // Check if payment has reached end date
        if (payment.getEndDate() != null && LocalDate.now().isAfter(payment.getEndDate())) {
            payment.setStatus(RecurringPaymentStatus.COMPLETED);
            recurringPaymentRepository.save(payment);
            unschedulePayment(payment);
            publishPaymentEvent("recurring-payment.completed", payment);
            log.info("Recurring payment completed (end date reached): {}", paymentId);
            return;
        }

        try {
            // Publish payment execution event to payment service via Kafka
            RecurringPaymentEvent event = new RecurringPaymentEvent(
                    "recurring-payment.execute",
                    payment.getId(),
                    payment.getUserId(),
                    payment.getSourceAccountId(),
                    payment.getDestinationAccountId(),
                    payment.getAmount(),
                    payment.getCurrency()
            );
            event.setFrequency(payment.getFrequency());
            event.setExecutionDate(LocalDate.now());

            kafkaTemplate.send("payment-events", "recurring-payment.execute", event);

            // Update payment state
            payment.setLastExecutionDate(LocalDate.now());
            payment.setNextExecutionDate(calculateNextExecutionDate(payment.getFrequency(), LocalDate.now()));
            payment.setCurrentRetryCount(0);
            recurringPaymentRepository.save(payment);

            publishPaymentEvent("recurring-payment.executed", payment);
            log.info("Recurring payment executed successfully: {}", paymentId);

        } catch (Exception e) {
            log.error("Failed to execute recurring payment: {}", paymentId, e);

            payment.setCurrentRetryCount(payment.getCurrentRetryCount() + 1);

            if (payment.getCurrentRetryCount() >= payment.getMaxRetries()) {
                payment.setStatus(RecurringPaymentStatus.FAILED);
                unschedulePayment(payment);
                publishPaymentEvent("recurring-payment.failed", payment);
                log.error("Recurring payment failed after max retries: {}", paymentId);
            }

            recurringPaymentRepository.save(payment);
            throw new JobSchedulingException("Payment execution failed", paymentId, e);
        }
    }

    /**
     * Schedule payment with Quartz using cron expression
     */
    private void schedulePaymentWithQuartz(RecurringPayment payment) {
        try {
            JobKey jobKey = new JobKey(payment.getId(), RECURRING_PAYMENT_GROUP);

            // Delete existing job if present
            if (quartzScheduler.checkExists(jobKey)) {
                quartzScheduler.deleteJob(jobKey);
            }

            JobDetail jobDetail = JobBuilder.newJob(RecurringPaymentJob.class)
                    .withIdentity(jobKey)
                    .usingJobData("paymentId", payment.getId())
                    .storeDurably()
                    .build();

            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(payment.getId() + "_trigger", RECURRING_PAYMENT_GROUP)
                    .withSchedule(CronScheduleBuilder.cronSchedule(payment.getFrequency().getCronExpression())
                            .withMisfireHandlingInstructionFireAndProceed())
                    .build();

            quartzScheduler.scheduleJob(jobDetail, trigger);
            payment.setQuartzJobKey(jobKey.toString());
            log.info("Recurring payment scheduled with Quartz: {}", payment.getId());

        } catch (SchedulerException e) {
            log.error("Failed to schedule recurring payment: {}", payment.getId(), e);
            throw new JobSchedulingException("Failed to schedule recurring payment", payment.getId(), e);
        }
    }

    /**
     * Unschedule a payment from Quartz
     */
    private void unschedulePayment(RecurringPayment payment) {
        try {
            JobKey jobKey = new JobKey(payment.getId(), RECURRING_PAYMENT_GROUP);
            if (quartzScheduler.checkExists(jobKey)) {
                quartzScheduler.deleteJob(jobKey);
                log.info("Recurring payment unscheduled: {}", payment.getId());
            }
        } catch (SchedulerException e) {
            log.warn("Failed to unschedule payment from Quartz: {}", payment.getId(), e);
        }
    }

    /**
     * Calculate the next execution date based on frequency
     */
    private LocalDate calculateNextExecutionDate(PaymentFrequency frequency, LocalDate fromDate) {
        return switch (frequency) {
            case DAILY -> fromDate.plusDays(1);
            case WEEKLY -> fromDate.plusWeeks(1);
            case BIWEEKLY -> fromDate.plusWeeks(2);
            case MONTHLY -> fromDate.plusMonths(1);
            case QUARTERLY -> fromDate.plusMonths(3);
            case YEARLY -> fromDate.plusYears(1);
        };
    }

    /**
     * Publish payment event to Kafka
     */
    private void publishPaymentEvent(String eventType, RecurringPayment payment) {
        try {
            RecurringPaymentEvent event = new RecurringPaymentEvent(
                    eventType,
                    payment.getId(),
                    payment.getUserId(),
                    payment.getSourceAccountId(),
                    payment.getDestinationAccountId(),
                    payment.getAmount(),
                    payment.getCurrency()
            );
            event.setFrequency(payment.getFrequency());
            event.setStatus(payment.getStatus());

            kafkaTemplate.send("scheduler-events", eventType, event);
            log.debug("Published event: {} for payment: {}", eventType, payment.getId());
        } catch (Exception e) {
            log.error("Failed to publish event: {} for payment: {}", eventType, payment.getId(), e);
        }
    }

    /**
     * Convert entity to response DTO
     */
    private RecurringPaymentResponse convertToResponse(RecurringPayment payment) {
        RecurringPaymentResponse response = new RecurringPaymentResponse();
        response.setId(payment.getId());
        response.setUserId(payment.getUserId());
        response.setSourceAccountId(payment.getSourceAccountId());
        response.setDestinationAccountId(payment.getDestinationAccountId());
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setFrequency(payment.getFrequency());
        response.setNextExecutionDate(payment.getNextExecutionDate());
        response.setLastExecutionDate(payment.getLastExecutionDate());
        response.setStartDate(payment.getStartDate());
        response.setEndDate(payment.getEndDate());
        response.setStatus(payment.getStatus());
        response.setMaxRetries(payment.getMaxRetries());
        response.setCurrentRetryCount(payment.getCurrentRetryCount());
        response.setDescription(payment.getDescription());
        response.setCreatedAt(payment.getCreatedAt());
        response.setUpdatedAt(payment.getUpdatedAt());
        return response;
    }
}
