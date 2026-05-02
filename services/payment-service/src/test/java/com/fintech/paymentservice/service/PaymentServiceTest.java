package com.fintech.paymentservice.service;

import com.fintech.paymentservice.dto.request.InitiateRequest;
import com.fintech.paymentservice.dto.response.PaymentInitiatedResponse;
import com.fintech.paymentservice.entity.Account;
import com.fintech.paymentservice.exception.AccountNotFoundException;
import com.fintech.paymentservice.exception.InsufficientFundsException;
import com.fintech.paymentservice.fraud.model.FraudScreeningResult;
import com.fintech.paymentservice.fraud.service.FraudDetectionService;
import com.fintech.paymentservice.messaging.OtpEmailPublisher;
import com.fintech.paymentservice.model.PaymentStatus;
import com.fintech.paymentservice.repository.AccountRepository;
import com.fintech.paymentservice.repository.PaymentRepository;
import com.fintech.paymentservice.util.SnowflakeIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private OtpEmailPublisher otpEmailPublisher;

    @Mock
    private OtpService otpService;

    @Mock
    private SnowflakeIdGenerator idGenerator;

    @Mock
    private BalanceService balanceService;

    @Mock
    private TransactionLimitService transactionLimitService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private FraudDetectionService fraudDetectionService;

    @InjectMocks
    private PaymentService paymentService;

    private InitiateRequest validRequest;
    private Account senderAccount;

    @BeforeEach
    void setUp() {
        validRequest = new InitiateRequest("ACC-FROM-001", "ACC-TO-002", new BigDecimal("250.00"), "Test payment");
        senderAccount = new Account("user-001", "ACC-FROM-001", new BigDecimal("5000.00"));
        senderAccount.setAvailableBalance(new BigDecimal("5000.00"));
    }

    @Test
    @DisplayName("should successfully initiate payment when all conditions are met")
    void shouldInitiatePaymentSuccessfully() {
        // Arrange
        when(balanceService.hasSufficientFunds("ACC-FROM-001", new BigDecimal("250.00"))).thenReturn(true);
        when(accountRepository.findByAccountNumber("ACC-FROM-001")).thenReturn(Optional.of(senderAccount));
        when(fraudDetectionService.screenTransaction(any()))
                .thenReturn(FraudScreeningResult.approve("PAY-123456", 10, java.util.List.of()));
        when(idGenerator.generateStringId()).thenReturn("PAY-123456");
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(otpService.generateOtp(anyString())).thenReturn("123456");

        // Act
        PaymentInitiatedResponse response = paymentService.initiate(validRequest, "user-001");

        // Assert
        assertNotNull(response);
        assertEquals("PAY-123456", response.paymentId());
        assertEquals("ACC-FROM-001", response.fromAccount());
        assertEquals("ACC-TO-002", response.toAccount());
        assertEquals(new BigDecimal("250.00"), response.amount());
        assertEquals(PaymentStatus.PENDING_VERIFICATION, response.status());
        verify(paymentRepository).save(any());
        verify(otpService).generateOtp("PAY-123456");
    }

    @Test
    @DisplayName("should throw InsufficientFundsException when sender has insufficient balance")
    void shouldThrowWhenInsufficientFunds() {
        // Arrange
        when(balanceService.hasSufficientFunds("ACC-FROM-001", new BigDecimal("250.00"))).thenReturn(false);

        // Act & Assert
        assertThrows(InsufficientFundsException.class,
                () -> paymentService.initiate(validRequest, "user-001"));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("should throw AccountNotFoundException when sender account does not exist")
    void shouldThrowWhenFromAccountNotFound() {
        // Arrange
        when(balanceService.hasSufficientFunds("ACC-FROM-001", new BigDecimal("250.00"))).thenReturn(true);
        when(accountRepository.findByAccountNumber("ACC-FROM-001")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AccountNotFoundException.class,
                () -> paymentService.initiate(validRequest, "user-001"));
        verify(paymentRepository, never()).save(any());
    }
}
