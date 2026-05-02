package com.fintech.retryservice.service;

import com.fintech.retryservice.dto.RetryRequest;
import com.fintech.retryservice.dto.RetryResponse;
import com.fintech.retryservice.dto.RetryStatusUpdate;
import com.fintech.retryservice.model.RetryAttempt;
import com.fintech.retryservice.model.RetryStatus;
import com.fintech.retryservice.repository.RetryAttemptRepository;
import com.fintech.retryservice.util.SnowflakeIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Core service for managing retry attempts with exponential backoff,
 * HTTP execution, and Kafka-based dispatch.
 */
@Service
public class RetryService {

    private static final Logger logger = LoggerFactory.getLogger(RetryService.class);

    private final RetryAttemptRepository retryAttemptRepository;
    private final SnowflakeIdGenerator idGenerator;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final WebClient webClient;

    @Value("${retry.stuck.threshold.minutes:15}")
    private int stuckThresholdMinutes;

    @Value("${retry.cleanup.days:30}")
    private int cleanupDays;

    @Value("${retry.kafka.topic.outbound:retry-outbound}")
    private String outboundTopic;

    public RetryService(RetryAttemptRepository retryAttemptRepository,
                        SnowflakeIdGenerator idGenerator,
                        KafkaTemplate<String, Object> kafkaTemplate,
                        WebClient authzWebClient) {
        this.retryAttemptRepository = retryAttemptRepository;
        this.idGenerator = idGenerator;
        this.kafkaTemplate = kafkaTemplate;
        this.webClient = authzWebClient;
    }

    /**
     * Schedule a new retry attempt.
     */
    @Transactional
    public RetryResponse scheduleRetry(RetryRequest request) {
        logger.info("Scheduling retry for originalId={}, type={}, service={}",
                request.getOriginalId(), request.getRetryType(), request.getServiceName());

        RetryAttempt attempt = new RetryAttempt();
        attempt.setRetryId(idGenerator.nextId());
        attempt.setOriginalId(request.getOriginalId());
        attempt.setRetryType(request.getRetryType());
        attempt.setRetryStatus(RetryStatus.PENDING);
        attempt.setRetryCount(0);
        attempt.setMaxRetries(request.getMaxRetries() != null ? request.getMaxRetries() : 3);
        attempt.setRetryDelaySeconds(request.getRetryDelaySeconds() != null ? request.getRetryDelaySeconds() : 60);
        attempt.setNextRetryTime(request.getNextRetryTime() != null ? request.getNextRetryTime() : LocalDateTime.now().plusSeconds(attempt.getRetryDelaySeconds()));
        attempt.setPriority(request.getPriority() != null ? request.getPriority() : "NORMAL");
        attempt.setServiceName(request.getServiceName());
        attempt.setEndpointUrl(request.getEndpointUrl());
        attempt.setRetryData(request.getRetryData());
        attempt.setCreatedBy(request.getCreatedBy());

        RetryAttempt saved = retryAttemptRepository.save(attempt);
        logger.info("Retry scheduled with id={}", saved.getRetryId());
        return RetryResponse.fromEntity(saved);
    }

    /**
     * Find due retry tasks and execute them via HTTP or Kafka.
     */
    @Transactional
    public void executeRetries() {
        List<RetryAttempt> dueRetries = retryAttemptRepository.findRetryAttemptsReadyForExecution(
                RetryStatus.PENDING, LocalDateTime.now());

        if (dueRetries.isEmpty()) {
            return;
        }

        logger.info("Found {} retry attempts ready for execution", dueRetries.size());

        for (RetryAttempt attempt : dueRetries) {
            try {
                attempt.setRetryStatus(RetryStatus.IN_PROGRESS);
                attempt.setLastUpdatedBy("retry-scheduler");
                retryAttemptRepository.save(attempt);

                if (attempt.getEndpointUrl() != null && !attempt.getEndpointUrl().isBlank()) {
                    executeViaHttp(attempt);
                } else {
                    executeViaKafka(attempt);
                }
            } catch (Exception e) {
                logger.error("Error executing retry id={}: {}", attempt.getRetryId(), e.getMessage());
                handleResult(attempt.getRetryId(), false, e.getMessage());
            }
        }
    }

    /**
     * Execute retry via HTTP POST to the configured endpoint.
     */
    private void executeViaHttp(RetryAttempt attempt) {
        try {
            webClient.post()
                    .uri(attempt.getEndpointUrl())
                    .bodyValue(attempt.getRetryData() != null ? attempt.getRetryData() : Map.of())
                    .retrieve()
                    .toBodilessEntity()
                    .subscribe(
                            response -> {
                                logger.info("HTTP retry succeeded for id={}", attempt.getRetryId());
                                handleResult(attempt.getRetryId(), true, null);
                            },
                            error -> {
                                logger.warn("HTTP retry failed for id={}: {}", attempt.getRetryId(), error.getMessage());
                                handleResult(attempt.getRetryId(), false, error.getMessage());
                            }
                    );
        } catch (Exception e) {
            logger.error("HTTP execution error for id={}: {}", attempt.getRetryId(), e.getMessage());
            handleResult(attempt.getRetryId(), false, e.getMessage());
        }
    }

    /**
     * Execute retry via Kafka by publishing to the outbound topic.
     */
    private void executeViaKafka(RetryAttempt attempt) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("retryId", attempt.getRetryId());
            message.put("originalId", attempt.getOriginalId());
            message.put("retryType", attempt.getRetryType().name());
            message.put("serviceName", attempt.getServiceName());
            message.put("retryCount", attempt.getRetryCount());
            message.put("retryData", attempt.getRetryData());

            kafkaTemplate.send(outboundTopic, attempt.getOriginalId(), message);
            logger.info("Kafka retry dispatched for id={} to topic={}", attempt.getRetryId(), outboundTopic);
        } catch (Exception e) {
            logger.error("Kafka dispatch failed for id={}: {}", attempt.getRetryId(), e.getMessage());
            handleResult(attempt.getRetryId(), false, e.getMessage());
        }
    }

    /**
     * Handle the result of a retry execution — update status with exponential backoff on failure.
     */
    @Transactional
    public void handleResult(String retryId, boolean success, String error) {
        Optional<RetryAttempt> optAttempt = retryAttemptRepository.findById(retryId);
        if (optAttempt.isEmpty()) {
            logger.warn("Retry attempt not found for result handling: id={}", retryId);
            return;
        }

        RetryAttempt attempt = optAttempt.get();

        if (success) {
            attempt.setRetryStatus(RetryStatus.COMPLETED);
            attempt.setCompletedAt(LocalDateTime.now());
            attempt.setLastUpdatedBy("retry-service");
            logger.info("Retry completed successfully: id={}", retryId);
        } else {
            attempt.setErrorMessage(error);
            attempt.setLastErrorCode("RETRY_FAILED");
            attempt.incrementRetryCount(); // handles exponential backoff internally
            attempt.setLastUpdatedBy("retry-service");

            if (attempt.getRetryStatus() == RetryStatus.MAX_RETRIES_EXCEEDED) {
                logger.warn("Retry max attempts exceeded: id={}, count={}", retryId, attempt.getRetryCount());
            } else {
                attempt.setRetryStatus(RetryStatus.PENDING);
                logger.info("Retry rescheduled: id={}, nextRetry={}, count={}",
                        retryId, attempt.getNextRetryTime(), attempt.getRetryCount());
            }
        }

        retryAttemptRepository.save(attempt);
    }

    /**
     * Force immediate retry of a task regardless of next scheduled time.
     */
    @Transactional
    public RetryResponse forceRetryNow(String retryId) {
        RetryAttempt attempt = retryAttemptRepository.findById(retryId)
                .orElseThrow(() -> new NoSuchElementException("Retry attempt not found: " + retryId));

        if (attempt.getRetryStatus() == RetryStatus.COMPLETED || attempt.getRetryStatus() == RetryStatus.CANCELLED) {
            throw new IllegalStateException("Cannot force retry on a " + attempt.getRetryStatus() + " task");
        }

        attempt.setRetryStatus(RetryStatus.PENDING);
        attempt.setNextRetryTime(LocalDateTime.now());
        attempt.setLastUpdatedBy("force-retry");

        RetryAttempt saved = retryAttemptRepository.save(attempt);
        logger.info("Forced immediate retry for id={}", retryId);
        return RetryResponse.fromEntity(saved);
    }

    /**
     * Cancel a retry task.
     */
    @Transactional
    public RetryResponse cancelRetry(String retryId) {
        RetryAttempt attempt = retryAttemptRepository.findById(retryId)
                .orElseThrow(() -> new NoSuchElementException("Retry attempt not found: " + retryId));

        if (attempt.getRetryStatus() == RetryStatus.COMPLETED) {
            throw new IllegalStateException("Cannot cancel a completed retry task");
        }

        attempt.setRetryStatus(RetryStatus.CANCELLED);
        attempt.setLastUpdatedBy("cancel-request");

        RetryAttempt saved = retryAttemptRepository.save(attempt);
        logger.info("Retry cancelled: id={}", retryId);
        return RetryResponse.fromEntity(saved);
    }

    /**
     * Reset stuck retries that have been IN_PROGRESS for too long back to PENDING.
     */
    @Transactional
    public int resetStuckRetries() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(stuckThresholdMinutes);
        int updated = retryAttemptRepository.updateStuckRetryAttempts(
                RetryStatus.IN_PROGRESS, RetryStatus.PENDING, threshold, LocalDateTime.now());
        if (updated > 0) {
            logger.info("Reset {} stuck retry attempts back to PENDING", updated);
        }
        return updated;
    }

    /**
     * Clean up old completed/cancelled/max-exceeded records.
     */
    @Transactional
    public void cleanupOldRecords() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(cleanupDays);
        List<RetryStatus> statuses = List.of(RetryStatus.COMPLETED, RetryStatus.CANCELLED, RetryStatus.MAX_RETRIES_EXCEEDED);
        retryAttemptRepository.deleteOldRetryAttempts(statuses, cutoff);
        logger.info("Cleaned up old retry records older than {} days", cleanupDays);
    }

    /**
     * Get a single retry attempt by ID.
     */
    public RetryResponse getRetryById(String retryId) {
        RetryAttempt attempt = retryAttemptRepository.findById(retryId)
                .orElseThrow(() -> new NoSuchElementException("Retry attempt not found: " + retryId));
        return RetryResponse.fromEntity(attempt);
    }

    /**
     * Get paginated retry attempts filtered by status.
     */
    public Page<RetryResponse> getRetriesByStatus(RetryStatus status, Pageable pageable) {
        Page<RetryAttempt> page = retryAttemptRepository.findByRetryStatus(status, pageable);
        return page.map(RetryResponse::fromEntity);
    }

    /**
     * Get all retries paginated (no status filter).
     */
    public Page<RetryResponse> getAllRetries(Pageable pageable) {
        Page<RetryAttempt> page = retryAttemptRepository.findAll(pageable);
        return page.map(RetryResponse::fromEntity);
    }

    /**
     * Update status via external callback.
     */
    @Transactional
    public RetryResponse updateStatus(String retryId, RetryStatusUpdate update) {
        RetryAttempt attempt = retryAttemptRepository.findById(retryId)
                .orElseThrow(() -> new NoSuchElementException("Retry attempt not found: " + retryId));

        if (update.getRetryStatus() != null) {
            attempt.setRetryStatus(update.getRetryStatus());
        }
        if (update.getErrorMessage() != null) {
            attempt.setErrorMessage(update.getErrorMessage());
        }
        if (update.getLastErrorCode() != null) {
            attempt.setLastErrorCode(update.getLastErrorCode());
        }
        if (update.getCompletedAt() != null) {
            attempt.setCompletedAt(update.getCompletedAt());
        }
        if (update.getUpdatedBy() != null) {
            attempt.setLastUpdatedBy(update.getUpdatedBy());
        }

        RetryAttempt saved = retryAttemptRepository.save(attempt);
        logger.info("Retry status updated: id={}, status={}", retryId, saved.getRetryStatus());
        return RetryResponse.fromEntity(saved);
    }

    /**
     * Get retry statistics grouped by type and status.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();

        stats.put("pending", retryAttemptRepository.countByRetryStatus(RetryStatus.PENDING));
        stats.put("inProgress", retryAttemptRepository.countByRetryStatus(RetryStatus.IN_PROGRESS));
        stats.put("completed", retryAttemptRepository.countByRetryStatus(RetryStatus.COMPLETED));
        stats.put("failed", retryAttemptRepository.countByRetryStatus(RetryStatus.FAILED));
        stats.put("cancelled", retryAttemptRepository.countByRetryStatus(RetryStatus.CANCELLED));
        stats.put("maxRetriesExceeded", retryAttemptRepository.countByRetryStatus(RetryStatus.MAX_RETRIES_EXCEEDED));

        List<Object[]> rawStats = retryAttemptRepository.getRetryStatistics();
        List<Map<String, Object>> breakdown = new ArrayList<>();
        for (Object[] row : rawStats) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("type", row[0]);
            entry.put("status", row[1]);
            entry.put("count", row[2]);
            breakdown.add(entry);
        }
        stats.put("breakdown", breakdown);
        stats.put("timestamp", LocalDateTime.now());

        return stats;
    }
}
