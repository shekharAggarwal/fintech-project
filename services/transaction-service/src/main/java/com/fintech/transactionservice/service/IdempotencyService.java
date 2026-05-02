package com.fintech.transactionservice.service;

import com.fintech.transactionservice.entity.Transaction;
import com.fintech.transactionservice.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Dual-layer idempotency check: Redis SETNX (fast) + DB fallback (durable).
 * Prevents duplicate transaction processing from REST and Kafka event paths.
 */
@Service
public class IdempotencyService {

    private static final Logger logger = LoggerFactory.getLogger(IdempotencyService.class);
    private static final String REDIS_KEY_PREFIX = "idempotency:txn:";
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;
    private final TransactionRepository transactionRepository;

    public IdempotencyService(StringRedisTemplate redisTemplate, TransactionRepository transactionRepository) {
        this.redisTemplate = redisTemplate;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Checks if a transaction with this idempotency key already exists.
     * Returns the existing transaction if found, empty otherwise.
     */
    public Optional<Transaction> checkDuplicate(String idempotencyKey) {
        // Layer 1: Redis fast check
        String cachedTxnId = null;
        try {
            cachedTxnId = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + idempotencyKey);
        } catch (Exception e) {
            logger.warn("Redis lookup failed for idempotency key: {}, falling back to DB", idempotencyKey, e);
        }

        if (cachedTxnId != null) {
            logger.info("Idempotency hit in Redis for key: {}, txnId: {}", idempotencyKey, cachedTxnId);
            return transactionRepository.findById(cachedTxnId);
        }

        // Layer 2: DB fallback
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            logger.info("Idempotency hit in DB for key: {}, txnId: {}", idempotencyKey, existing.get().getTxnId());
            // Backfill Redis cache
            storeInRedis(idempotencyKey, existing.get().getTxnId());
        }

        return existing;
    }

    /**
     * Attempts to reserve the idempotency key in Redis using SETNX.
     * Returns true if the key was successfully reserved (no duplicate), false otherwise.
     */
    public boolean tryReserve(String idempotencyKey, String txnId) {
        try {
            Boolean success = redisTemplate.opsForValue().setIfAbsent(
                    REDIS_KEY_PREFIX + idempotencyKey, txnId, TTL);
            if (Boolean.TRUE.equals(success)) {
                logger.debug("Idempotency key reserved in Redis: {} -> {}", idempotencyKey, txnId);
                return true;
            } else {
                logger.info("Idempotency key already reserved in Redis: {}", idempotencyKey);
                return false;
            }
        } catch (Exception e) {
            logger.warn("Redis SETNX failed for idempotency key: {}, proceeding with DB check", idempotencyKey, e);
            // If Redis is down, we rely on the DB unique constraint as fallback
            return true;
        }
    }

    /**
     * Stores the idempotency key -> txnId mapping in Redis with TTL.
     */
    public void storeInRedis(String idempotencyKey, String txnId) {
        try {
            redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + idempotencyKey, txnId, TTL);
            logger.debug("Stored idempotency mapping in Redis: {} -> {}", idempotencyKey, txnId);
        } catch (Exception e) {
            logger.warn("Failed to store idempotency key in Redis: {} -> {}", idempotencyKey, txnId, e);
        }
    }
}
