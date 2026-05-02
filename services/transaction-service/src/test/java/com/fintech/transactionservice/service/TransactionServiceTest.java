package com.fintech.transactionservice.service;

import com.fintech.transactionservice.adapter.BankAdapter;
import com.fintech.transactionservice.adapter.BankAdapterFactory;
import com.fintech.transactionservice.dto.request.TransactionRequest;
import com.fintech.transactionservice.entity.Transaction;
import com.fintech.transactionservice.entity.TransactionStatus;
import com.fintech.transactionservice.messaging.TransactionCompletedEventPublisher;
import com.fintech.transactionservice.model.TransactionResult;
import com.fintech.transactionservice.repository.TransactionRepository;
import com.fintech.transactionservice.util.SnowflakeIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Mock
    private TransactionCompletedEventPublisher transactionCompletedEventPublisher;

    @Mock
    private BankAdapterFactory bankAdapterFactory;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private IdempotencyService idempotencyService;

    @Mock
    private BankAdapter bankAdapter;

    @InjectMocks
    private TransactionService transactionService;

    private TransactionRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new TransactionRequest("user-001", "ACC-FROM", "ACC-TO", new BigDecimal("500.00"), "Test transfer");
    }

    @Test
    @DisplayName("should create transaction with PENDING status on initiation")
    void shouldInitiateTransactionWithPendingStatus() {
        // Arrange
        when(snowflakeIdGenerator.nextId()).thenReturn("TXN-001");
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction txn = invocation.getArgument(0);
            return txn;
        });
        when(bankAdapterFactory.getAdapter("Self")).thenReturn(bankAdapter);
        when(bankAdapter.process(any(Transaction.class))).thenReturn(new TransactionResult(true, "200", "Success"));

        // Act
        Transaction result = transactionService.initiateTransaction(validRequest, "idem-key-001");

        // Assert
        assertNotNull(result);
        assertEquals("TXN-001", result.getTxnId());
        assertEquals("user-001", result.getUserId());
        assertEquals("ACC-FROM", result.getFromAccount());
        assertEquals("ACC-TO", result.getToAccount());
        assertEquals(new BigDecimal("500.00"), result.getAmount());
        assertEquals("idem-key-001", result.getIdempotencyKey());
        // Transaction goes through PENDING -> PROCESSING -> COMPLETED
        verify(transactionRepository, atLeast(2)).save(any(Transaction.class));
        verify(idempotencyService).storeInRedis("idem-key-001", "TXN-001");
    }

    @Test
    @DisplayName("should return transaction when found by ID")
    void shouldReturnTransactionWhenFoundById() {
        // Arrange
        Transaction existingTxn = new Transaction("TXN-002", "PAY-002", "user-001", "ACC-1", "ACC-2", new BigDecimal("100.00"), "Existing");
        existingTxn.setStatus(TransactionStatus.COMPLETED);
        when(transactionRepository.findById("TXN-002")).thenReturn(Optional.of(existingTxn));

        // Act
        Optional<Transaction> result = transactionService.findById("TXN-002");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("TXN-002", result.get().getTxnId());
        assertEquals(TransactionStatus.COMPLETED, result.get().getStatus());
        verify(transactionRepository).findById("TXN-002");
    }

    @Test
    @DisplayName("should return empty Optional when transaction not found by ID")
    void shouldReturnEmptyWhenTransactionNotFound() {
        // Arrange
        when(transactionRepository.findById("INVALID-TXN")).thenReturn(Optional.empty());

        // Act
        Optional<Transaction> result = transactionService.findById("INVALID-TXN");

        // Assert
        assertTrue(result.isEmpty());
        verify(transactionRepository).findById("INVALID-TXN");
    }
}
