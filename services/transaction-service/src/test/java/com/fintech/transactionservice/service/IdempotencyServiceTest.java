package com.fintech.transactionservice.service;

import com.fintech.transactionservice.entity.Transaction;
import com.fintech.transactionservice.entity.TransactionStatus;
import com.fintech.transactionservice.repository.TransactionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IdempotencyServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private IdempotencyService idempotencyService;

    @Test
    @DisplayName("should return empty when first request with new idempotency key")
    void shouldReturnEmptyForNewIdempotencyKey() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("idempotency:new-key")).thenReturn(null);
        when(transactionRepository.findByIdempotencyKey("new-key")).thenReturn(Optional.empty());

        // Act
        Optional<Transaction> result = idempotencyService.checkDuplicate("new-key");

        // Assert
        assertTrue(result.isEmpty());
        verify(valueOperations).get("idempotency:new-key");
        verify(transactionRepository).findByIdempotencyKey("new-key");
    }

    @Test
    @DisplayName("should return existing transaction when duplicate key found in Redis")
    void shouldReturnExistingTransactionWhenDuplicateInRedis() {
        // Arrange
        Transaction existingTxn = new Transaction("TXN-100", "PAY-100", "user-001", "ACC-1", "ACC-2",
                new BigDecimal("200.00"), "Duplicate test");
        existingTxn.setStatus(TransactionStatus.COMPLETED);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("idempotency:existing-key")).thenReturn("TXN-100");
        when(transactionRepository.findById("TXN-100")).thenReturn(Optional.of(existingTxn));

        // Act
        Optional<Transaction> result = idempotencyService.checkDuplicate("existing-key");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("TXN-100", result.get().getTxnId());
        assertEquals(TransactionStatus.COMPLETED, result.get().getStatus());
        // Should NOT query DB by idempotency key since Redis had the answer
        verify(transactionRepository, never()).findByIdempotencyKey(anyString());
    }

    @Test
    @DisplayName("should fall through to DB check when Redis fails")
    void shouldFallThroughToDbWhenRedisFails() {
        // Arrange
        Transaction dbTxn = new Transaction("TXN-200", "PAY-200", "user-002", "ACC-3", "ACC-4",
                new BigDecimal("300.00"), "DB fallback test");
        dbTxn.setStatus(TransactionStatus.PENDING);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("idempotency:redis-fail-key"))
                .thenThrow(new RedisConnectionFailureException("Connection refused"));
        when(transactionRepository.findByIdempotencyKey("redis-fail-key")).thenReturn(Optional.of(dbTxn));

        // Act
        Optional<Transaction> result = idempotencyService.checkDuplicate("redis-fail-key");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("TXN-200", result.get().getTxnId());
        verify(transactionRepository).findByIdempotencyKey("redis-fail-key");
    }

    @Test
    @DisplayName("should return empty when Redis fails and DB has no record")
    void shouldReturnEmptyWhenRedisFailsAndDbEmpty() {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("idempotency:no-record-key"))
                .thenThrow(new RedisConnectionFailureException("Connection refused"));
        when(transactionRepository.findByIdempotencyKey("no-record-key")).thenReturn(Optional.empty());

        // Act
        Optional<Transaction> result = idempotencyService.checkDuplicate("no-record-key");

        // Assert
        assertTrue(result.isEmpty());
    }
}
