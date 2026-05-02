package com.fintech.ledgerservice.service;

import com.fintech.ledgerservice.dto.message.TransactionCompletedMessage;
import com.fintech.ledgerservice.dto.response.AccountBalanceResponse;
import com.fintech.ledgerservice.entity.LedgerEntry;
import com.fintech.ledgerservice.entity.LedgerEntryType;
import com.fintech.ledgerservice.repository.LedgerRepository;
import com.fintech.ledgerservice.util.SnowflakeIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock
    private LedgerRepository ledgerRepo;

    @Mock
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @InjectMocks
    private LedgerService ledgerService;

    @Captor
    private ArgumentCaptor<LedgerEntry> ledgerEntryCaptor;

    private TransactionCompletedMessage validMessage;

    @BeforeEach
    void setUp() {
        validMessage = new TransactionCompletedMessage(
                "TXN-001", "PAY-001", "user-001",
                "ACC-SENDER", "ACC-RECEIVER",
                new BigDecimal("750.00"), "Transfer payment", "COMPLETED"
        );
    }

    @Nested
    @DisplayName("createDoubleEntry()")
    class CreateDoubleEntryTests {

        @Test
        @DisplayName("should create both DEBIT and CREDIT entries")
        void shouldCreateDebitAndCreditEntries() {
            // Arrange
            when(snowflakeIdGenerator.nextId()).thenReturn("ENTRY-001", "ENTRY-002");
            when(ledgerRepo.save(any(LedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            ledgerService.createDoubleEntry(
                    validMessage.getTxnId(), validMessage.getFromAccount(), validMessage.getToAccount(),
                    validMessage.getAmount(), validMessage.getPaymentId(), validMessage.getDescription());

            // Assert
            verify(ledgerRepo, times(2)).save(ledgerEntryCaptor.capture());
            List<LedgerEntry> savedEntries = ledgerEntryCaptor.getAllValues();

            assertEquals(2, savedEntries.size());

            // First entry should be DEBIT (sender)
            LedgerEntry debitEntry = savedEntries.get(0);
            assertEquals(LedgerEntryType.DEBIT, debitEntry.getEntryType());
            assertEquals("ACC-SENDER", debitEntry.getAccountNumber());
            assertEquals(new BigDecimal("750.00"), debitEntry.getAmount());

            // Second entry should be CREDIT (receiver)
            LedgerEntry creditEntry = savedEntries.get(1);
            assertEquals(LedgerEntryType.CREDIT, creditEntry.getEntryType());
            assertEquals("ACC-RECEIVER", creditEntry.getAccountNumber());
            assertEquals(new BigDecimal("750.00"), creditEntry.getAmount());
        }

        @Test
        @DisplayName("should share the same transactionId for both entries")
        void shouldShareSameTransactionId() {
            // Arrange
            when(snowflakeIdGenerator.nextId()).thenReturn("ENTRY-A", "ENTRY-B");
            when(ledgerRepo.save(any(LedgerEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            ledgerService.createDoubleEntry(
                    validMessage.getTxnId(), validMessage.getFromAccount(), validMessage.getToAccount(),
                    validMessage.getAmount(), validMessage.getPaymentId(), validMessage.getDescription());

            // Assert
            verify(ledgerRepo, times(2)).save(ledgerEntryCaptor.capture());
            List<LedgerEntry> entries = ledgerEntryCaptor.getAllValues();

            String txnId = entries.get(0).getTxnId();
            assertEquals(txnId, entries.get(1).getTxnId());
            assertEquals("TXN-001", txnId);
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when amount is null or non-positive")
        void shouldThrowWhenAmountIsInvalid() {
            // Arrange
            TransactionCompletedMessage zeroMsg = new TransactionCompletedMessage(
                    "TXN-002", "PAY-002", "user-001",
                    "ACC-1", "ACC-2", BigDecimal.ZERO, "Zero amount", "COMPLETED");

            TransactionCompletedMessage negativeMsg = new TransactionCompletedMessage(
                    "TXN-003", "PAY-003", "user-001",
                    "ACC-1", "ACC-2", new BigDecimal("-100.00"), "Negative amount", "COMPLETED");

            TransactionCompletedMessage nullMsg = new TransactionCompletedMessage(
                    "TXN-004", "PAY-004", "user-001",
                    "ACC-1", "ACC-2", null, "Null amount", "COMPLETED");

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> ledgerService.createDoubleEntry(
                    zeroMsg.getTxnId(), zeroMsg.getFromAccount(), zeroMsg.getToAccount(),
                    zeroMsg.getAmount(), zeroMsg.getPaymentId(), zeroMsg.getDescription()));
            assertThrows(IllegalArgumentException.class, () -> ledgerService.createDoubleEntry(
                    negativeMsg.getTxnId(), negativeMsg.getFromAccount(), negativeMsg.getToAccount(),
                    negativeMsg.getAmount(), negativeMsg.getPaymentId(), negativeMsg.getDescription()));
            assertThrows(IllegalArgumentException.class, () -> ledgerService.createDoubleEntry(
                    nullMsg.getTxnId(), nullMsg.getFromAccount(), nullMsg.getToAccount(),
                    nullMsg.getAmount(), nullMsg.getPaymentId(), nullMsg.getDescription()));
            verify(ledgerRepo, never()).save(any());
        }
    }

    @Nested
    @DisplayName("reconcile()")
    class ReconcileTests {

        @Test
        @DisplayName("should return true when total DEBIT equals total CREDIT")
        void shouldReturnTrueWhenBalanced() {
            // Arrange
            when(ledgerRepo.sumAmountByTxnIdAndEntryType("TXN-001", LedgerEntryType.DEBIT))
                    .thenReturn(new BigDecimal("750.00"));
            when(ledgerRepo.sumAmountByTxnIdAndEntryType("TXN-001", LedgerEntryType.CREDIT))
                    .thenReturn(new BigDecimal("750.00"));

            // Act
            boolean result = ledgerService.reconcile("TXN-001");

            // Assert
            assertTrue(result);
        }

        @Test
        @DisplayName("should return false when total DEBIT does not equal total CREDIT")
        void shouldReturnFalseWhenUnbalanced() {
            // Arrange
            when(ledgerRepo.sumAmountByTxnIdAndEntryType("TXN-001", LedgerEntryType.DEBIT))
                    .thenReturn(new BigDecimal("750.00"));
            when(ledgerRepo.sumAmountByTxnIdAndEntryType("TXN-001", LedgerEntryType.CREDIT))
                    .thenReturn(new BigDecimal("500.00"));

            // Act
            boolean result = ledgerService.reconcile("TXN-001");

            // Assert
            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("getAccountBalance()")
    class GetAccountBalanceTests {

        @Test
        @DisplayName("should calculate balance as CREDIT minus DEBIT")
        void shouldCalculateBalanceCorrectly() {
            // Arrange
            when(ledgerRepo.sumAmountByAccountNumberAndEntryType("ACC-001", LedgerEntryType.CREDIT))
                    .thenReturn(new BigDecimal("5000.00"));
            when(ledgerRepo.sumAmountByAccountNumberAndEntryType("ACC-001", LedgerEntryType.DEBIT))
                    .thenReturn(new BigDecimal("2000.00"));

            // Act
            AccountBalanceResponse response = ledgerService.getAccountBalance("ACC-001");

            // Assert
            assertEquals(new BigDecimal("3000.00"), response.getCurrentBalance());
            assertEquals("ACC-001", response.getAccountId());
            assertEquals("USD", response.getCurrency());
        }

        @Test
        @DisplayName("should return zero balance when no entries exist")
        void shouldReturnZeroWhenNoEntries() {
            // Arrange
            when(ledgerRepo.sumAmountByAccountNumberAndEntryType("NEW-ACC", LedgerEntryType.CREDIT))
                    .thenReturn(BigDecimal.ZERO);
            when(ledgerRepo.sumAmountByAccountNumberAndEntryType("NEW-ACC", LedgerEntryType.DEBIT))
                    .thenReturn(BigDecimal.ZERO);

            // Act
            AccountBalanceResponse response = ledgerService.getAccountBalance("NEW-ACC");

            // Assert
            assertEquals(BigDecimal.ZERO, response.getCurrentBalance());
        }

        @Test
        @DisplayName("should return negative balance when debits exceed credits")
        void shouldReturnNegativeBalanceWhenOverdrawn() {
            // Arrange
            when(ledgerRepo.sumAmountByAccountNumberAndEntryType("OVERDRAWN-ACC", LedgerEntryType.CREDIT))
                    .thenReturn(new BigDecimal("1000.00"));
            when(ledgerRepo.sumAmountByAccountNumberAndEntryType("OVERDRAWN-ACC", LedgerEntryType.DEBIT))
                    .thenReturn(new BigDecimal("1500.00"));

            // Act
            AccountBalanceResponse response = ledgerService.getAccountBalance("OVERDRAWN-ACC");

            // Assert
            assertEquals(new BigDecimal("-500.00"), response.getCurrentBalance());
        }
    }
}
