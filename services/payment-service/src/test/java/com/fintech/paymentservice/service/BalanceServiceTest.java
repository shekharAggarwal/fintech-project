package com.fintech.paymentservice.service;

import com.fintech.paymentservice.dto.response.BalanceResponse;
import com.fintech.paymentservice.entity.Account;
import com.fintech.paymentservice.exception.AccountNotFoundException;
import com.fintech.paymentservice.exception.InsufficientFundsException;
import com.fintech.paymentservice.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private BalanceService balanceService;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        testAccount = new Account("user-001", "1234567890", new BigDecimal("1000.00"));
        testAccount.setAvailableBalance(new BigDecimal("1000.00"));
        testAccount.setHoldAmount(BigDecimal.ZERO);
        testAccount.setCurrency("USD");
    }

    @Nested
    @DisplayName("credit()")
    class CreditTests {

        @Test
        @DisplayName("should increase currentBalance and availableBalance")
        void shouldIncreaseBothBalancesOnCredit() {
            // Arrange
            BigDecimal creditAmount = new BigDecimal("500.00");
            when(accountRepository.findByAccountNumber("acc-001")).thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            BalanceResponse response = balanceService.credit("acc-001", creditAmount, "ref-001");

            // Assert
            assertEquals(new BigDecimal("1500.00"), response.currentBalance());
            assertEquals(new BigDecimal("1500.00"), response.availableBalance());
            verify(accountRepository).findByAccountNumber("acc-001");
            verify(accountRepository).save(any(Account.class));
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when amount is zero or negative")
        void shouldThrowWhenCreditAmountIsInvalid() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> balanceService.credit("acc-001", BigDecimal.ZERO, "ref-001"));
            assertThrows(IllegalArgumentException.class,
                    () -> balanceService.credit("acc-001", new BigDecimal("-10.00"), "ref-001"));
            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw AccountNotFoundException when account does not exist")
        void shouldThrowWhenAccountNotFound() {
            // Arrange
            when(accountRepository.findByAccountNumber("invalid-acc")).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(AccountNotFoundException.class,
                    () -> balanceService.credit("invalid-acc", new BigDecimal("100.00"), "ref-001"));
        }
    }

    @Nested
    @DisplayName("debit()")
    class DebitTests {

        @Test
        @DisplayName("should decrease currentBalance and availableBalance")
        void shouldDecreaseBothBalancesOnDebit() {
            // Arrange
            BigDecimal debitAmount = new BigDecimal("300.00");
            when(accountRepository.findByAccountNumber("acc-001")).thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            BalanceResponse response = balanceService.debit("acc-001", debitAmount, "ref-002");

            // Assert
            assertEquals(new BigDecimal("700.00"), response.currentBalance());
            assertEquals(new BigDecimal("700.00"), response.availableBalance());
            verify(accountRepository).save(any(Account.class));
        }

        @Test
        @DisplayName("should throw InsufficientFundsException when available balance is less than debit amount")
        void shouldThrowWhenInsufficientFunds() {
            // Arrange
            BigDecimal debitAmount = new BigDecimal("2000.00");
            when(accountRepository.findByAccountNumber("acc-001")).thenReturn(Optional.of(testAccount));

            // Act & Assert
            assertThrows(InsufficientFundsException.class,
                    () -> balanceService.debit("acc-001", debitAmount, "ref-002"));
            verify(accountRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when debit amount is zero or negative")
        void shouldThrowWhenDebitAmountIsInvalid() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> balanceService.debit("acc-001", BigDecimal.ZERO, "ref-002"));
            assertThrows(IllegalArgumentException.class,
                    () -> balanceService.debit("acc-001", new BigDecimal("-50.00"), "ref-002"));
        }
    }

    @Nested
    @DisplayName("placeHold()")
    class PlaceHoldTests {

        @Test
        @DisplayName("should reduce availableBalance and increase holdAmount")
        void shouldReduceAvailableAndIncreaseHold() {
            // Arrange
            BigDecimal holdAmount = new BigDecimal("200.00");
            when(accountRepository.findByAccountNumber("acc-001")).thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            BalanceResponse response = balanceService.placeHold("acc-001", holdAmount, "ref-003");

            // Assert
            assertEquals(new BigDecimal("1000.00"), response.currentBalance()); // unchanged
            assertEquals(new BigDecimal("800.00"), response.availableBalance());
            assertEquals(new BigDecimal("200.00"), response.holdAmount());
            verify(accountRepository).save(any(Account.class));
        }

        @Test
        @DisplayName("should throw InsufficientFundsException when available balance is less than hold amount")
        void shouldThrowWhenInsufficientFundsForHold() {
            // Arrange
            BigDecimal holdAmount = new BigDecimal("1500.00");
            when(accountRepository.findByAccountNumber("acc-001")).thenReturn(Optional.of(testAccount));

            // Act & Assert
            assertThrows(InsufficientFundsException.class,
                    () -> balanceService.placeHold("acc-001", holdAmount, "ref-003"));
            verify(accountRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("releaseHold()")
    class ReleaseHoldTests {

        @Test
        @DisplayName("should increase availableBalance and decrease holdAmount")
        void shouldReverseHold() {
            // Arrange
            testAccount.setAvailableBalance(new BigDecimal("800.00"));
            testAccount.setHoldAmount(new BigDecimal("200.00"));
            BigDecimal releaseAmount = new BigDecimal("200.00");
            when(accountRepository.findByAccountNumber("acc-001")).thenReturn(Optional.of(testAccount));
            when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            BalanceResponse response = balanceService.releaseHold("acc-001", releaseAmount, "ref-004");

            // Assert
            assertEquals(new BigDecimal("1000.00"), response.availableBalance());
            assertEquals(new BigDecimal("0"), response.holdAmount().stripTrailingZeros());
            verify(accountRepository).save(any(Account.class));
        }

        @Test
        @DisplayName("should throw IllegalArgumentException when release amount exceeds hold amount")
        void shouldThrowWhenReleaseExceedsHold() {
            // Arrange
            testAccount.setHoldAmount(new BigDecimal("100.00"));
            BigDecimal releaseAmount = new BigDecimal("200.00");
            when(accountRepository.findByAccountNumber("acc-001")).thenReturn(Optional.of(testAccount));

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> balanceService.releaseHold("acc-001", releaseAmount, "ref-004"));
            verify(accountRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("hasSufficientFunds()")
    class HasSufficientFundsTests {

        @Test
        @DisplayName("should return true when available balance is greater than or equal to amount")
        void shouldReturnTrueWhenSufficientFunds() {
            // Arrange
            when(accountRepository.findByAccountNumber("acc-001")).thenReturn(Optional.of(testAccount));

            // Act & Assert
            assertTrue(balanceService.hasSufficientFunds("acc-001", new BigDecimal("1000.00")));
            assertTrue(balanceService.hasSufficientFunds("acc-001", new BigDecimal("500.00")));
        }

        @Test
        @DisplayName("should return false when available balance is less than amount")
        void shouldReturnFalseWhenInsufficientFunds() {
            // Arrange
            when(accountRepository.findByAccountNumber("acc-001")).thenReturn(Optional.of(testAccount));

            // Act & Assert
            assertFalse(balanceService.hasSufficientFunds("acc-001", new BigDecimal("1500.00")));
        }

        @Test
        @DisplayName("should throw AccountNotFoundException when account does not exist")
        void shouldThrowWhenAccountNotFoundForFundsCheck() {
            // Arrange
            when(accountRepository.findByAccountNumber("invalid")).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(AccountNotFoundException.class,
                    () -> balanceService.hasSufficientFunds("invalid", new BigDecimal("100.00")));
        }
    }
}
