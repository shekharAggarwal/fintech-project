package com.fintech.paymentservice.service;

import com.fintech.paymentservice.config.TransactionLimitProperties;
import com.fintech.paymentservice.dto.response.TransactionLimitResponse;
import com.fintech.paymentservice.entity.LimitType;
import com.fintech.paymentservice.entity.TransactionLimit;
import com.fintech.paymentservice.exception.TransactionLimitExceededException;
import com.fintech.paymentservice.repository.TransactionLimitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionLimitService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionLimitService.class);

    private final TransactionLimitRepository limitRepository;
    private final TransactionLimitProperties limitProperties;

    public TransactionLimitService(TransactionLimitRepository limitRepository,
                                    TransactionLimitProperties limitProperties) {
        this.limitRepository = limitRepository;
        this.limitProperties = limitProperties;
    }

    /**
     * Check if a transaction amount is within all limits for the account
     */
    @Transactional(readOnly = true)
    public void checkLimits(String accountId, BigDecimal amount) {
        logger.debug("Checking transaction limits for account {} amount {}", accountId, amount);

        List<TransactionLimit> limits = limitRepository.findByAccountIdAndEnabled(accountId, true);

        // If no limits exist and auto-create is enabled, create defaults
        if (limits.isEmpty() && limitProperties.isAutoCreateOnFirstTransaction()) {
            limits = createDefaultLimits(accountId);
        }

        for (TransactionLimit limit : limits) {
            // Per-transaction check
            if (limit.getLimitType() == LimitType.PER_TRANSACTION) {
                if (amount.compareTo(limit.getMaxAmount()) > 0) {
                    throw new TransactionLimitExceededException(
                        limit.getLimitType().name(), accountId,
                        "Per-transaction limit exceeded. Max: " + limit.getMaxAmount() + ", Requested: " + amount);
                }
            } else {
                // Cumulative limit check (daily/weekly/monthly)
                BigDecimal projectedUsage = limit.getCurrentUsage().add(amount);
                if (projectedUsage.compareTo(limit.getMaxAmount()) > 0) {
                    BigDecimal remaining = limit.getMaxAmount().subtract(limit.getCurrentUsage());
                    throw new TransactionLimitExceededException(
                        limit.getLimitType().name(), accountId,
                        limit.getLimitType() + " limit exceeded. Max: " + limit.getMaxAmount() +
                        ", Current usage: " + limit.getCurrentUsage() + ", Remaining: " + remaining);
                }
            }
        }

        logger.debug("All limits passed for account {} amount {}", accountId, amount);
    }

    /**
     * Record usage after a successful transaction
     */
    @Transactional
    public void recordUsage(String accountId, BigDecimal amount) {
        logger.debug("Recording usage for account {} amount {}", accountId, amount);

        List<TransactionLimit> limits = limitRepository.findByAccountIdAndEnabled(accountId, true);

        for (TransactionLimit limit : limits) {
            if (limit.getLimitType() != LimitType.PER_TRANSACTION) {
                limit.setCurrentUsage(limit.getCurrentUsage().add(amount));
                limitRepository.save(limit);
            }
        }
    }

    /**
     * Reset daily limits - runs every day at midnight
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void resetDailyLimits() {
        logger.info("Resetting daily transaction limits");
        Instant now = Instant.now();
        Instant nextReset = now.plus(1, ChronoUnit.DAYS);
        int count = limitRepository.resetLimitsByType(LimitType.DAILY, now, nextReset);
        logger.info("Reset {} daily limits", count);
    }

    /**
     * Reset weekly limits - runs every Monday at midnight
     */
    @Scheduled(cron = "0 0 0 * * MON")
    @Transactional
    public void resetWeeklyLimits() {
        logger.info("Resetting weekly transaction limits");
        Instant now = Instant.now();
        Instant nextReset = now.plus(7, ChronoUnit.DAYS);
        int count = limitRepository.resetLimitsByType(LimitType.WEEKLY, now, nextReset);
        logger.info("Reset {} weekly limits", count);
    }

    /**
     * Reset monthly limits - runs on 1st of each month
     */
    @Scheduled(cron = "0 0 0 1 * *")
    @Transactional
    public void resetMonthlyLimits() {
        logger.info("Resetting monthly transaction limits");
        Instant now = Instant.now();
        Instant nextReset = now.plus(30, ChronoUnit.DAYS);
        int count = limitRepository.resetLimitsByType(LimitType.MONTHLY, now, nextReset);
        logger.info("Reset {} monthly limits", count);
    }

    /**
     * Get all limits for an account
     */
    @Transactional(readOnly = true)
    public List<TransactionLimitResponse> getLimits(String accountId) {
        List<TransactionLimit> limits = limitRepository.findByAccountId(accountId);
        return limits.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Update a specific limit for an account
     */
    @Transactional
    public TransactionLimitResponse updateLimit(String accountId, LimitType limitType, BigDecimal maxAmount, Boolean enabled) {
        TransactionLimit limit = limitRepository.findByAccountIdAndLimitType(accountId, limitType)
            .orElseGet(() -> {
                TransactionLimit newLimit = new TransactionLimit(accountId, limitType, maxAmount, limitProperties.getDefaultCurrency());
                newLimit.setResetAt(calculateNextReset(limitType));
                return newLimit;
            });

        limit.setMaxAmount(maxAmount);
        if (enabled != null) {
            limit.setEnabled(enabled);
        }

        limit = limitRepository.save(limit);
        return toResponse(limit);
    }

    private List<TransactionLimit> createDefaultLimits(String accountId) {
        logger.info("Creating default transaction limits for account {}", accountId);

        String currency = limitProperties.getDefaultCurrency();
        Instant now = Instant.now();

        List<TransactionLimit> defaults = List.of(
            createLimit(accountId, LimitType.PER_TRANSACTION, limitProperties.getPerTransaction(), currency, null),
            createLimit(accountId, LimitType.DAILY, limitProperties.getDaily(), currency, now.plus(1, ChronoUnit.DAYS)),
            createLimit(accountId, LimitType.WEEKLY, limitProperties.getWeekly(), currency, now.plus(7, ChronoUnit.DAYS)),
            createLimit(accountId, LimitType.MONTHLY, limitProperties.getMonthly(), currency, now.plus(30, ChronoUnit.DAYS))
        );

        return limitRepository.saveAll(defaults);
    }

    private TransactionLimit createLimit(String accountId, LimitType type, BigDecimal maxAmount, String currency, Instant resetAt) {
        TransactionLimit limit = new TransactionLimit(accountId, type, maxAmount, currency);
        limit.setResetAt(resetAt);
        return limit;
    }

    private Instant calculateNextReset(LimitType type) {
        Instant now = Instant.now();
        return switch (type) {
            case DAILY -> now.plus(1, ChronoUnit.DAYS);
            case WEEKLY -> now.plus(7, ChronoUnit.DAYS);
            case MONTHLY -> now.plus(30, ChronoUnit.DAYS);
            case PER_TRANSACTION -> null;
        };
    }

    private TransactionLimitResponse toResponse(TransactionLimit limit) {
        BigDecimal remaining = limit.getMaxAmount().subtract(limit.getCurrentUsage());
        if (remaining.compareTo(BigDecimal.ZERO) < 0) remaining = BigDecimal.ZERO;

        return new TransactionLimitResponse(
            limit.getId(),
            limit.getAccountId(),
            limit.getLimitType(),
            limit.getMaxAmount(),
            limit.getCurrentUsage(),
            remaining,
            limit.getResetAt(),
            limit.getCurrency(),
            limit.isEnabled()
        );
    }
}
