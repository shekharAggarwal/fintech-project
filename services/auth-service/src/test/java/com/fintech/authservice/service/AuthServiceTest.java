package com.fintech.authservice.service;

import com.fintech.authservice.dto.response.AuthenticationResult;
import com.fintech.authservice.entity.AuthCore;
import com.fintech.authservice.messaging.EmailNotificationPublisher;
import com.fintech.authservice.messaging.SessionCreationKafkaPublisher;
import com.fintech.authservice.messaging.UserCreationKafkaPublisher;
import com.fintech.authservice.model.AuthCredDB;
import com.fintech.authservice.repository.AuthCoreRepository;
import com.fintech.authservice.repository.AuthCredentialsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fintech.authservice.util.SecurityUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthCoreRepository authCoreRepository;

    @Mock
    private AuthCredentialsRepository credentialsRepository;

    @Mock
    private UserCreationKafkaPublisher userCreationKafkaPublisher;

    @Mock
    private SessionService sessionService;

    @Mock
    private SessionCreationKafkaPublisher sessionCreationKafkaPublisher;

    @Mock
    private EmailNotificationPublisher emailNotificationPublisher;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private RateLimitingService rateLimitingService;

    @InjectMocks
    private AuthService authService;

    private AuthCore activeAuthCore;

    @BeforeEach
    void setUp() {
        activeAuthCore = new AuthCore("user-001", "test@example.com");
        activeAuthCore.setStatus(AuthCore.AuthStatus.ACTIVE);
        activeAuthCore.setId(1L);
    }

    @Test
    @DisplayName("should return successful authentication result with tokens on valid login")
    void shouldReturnTokensOnSuccessfulLogin() {
        // Arrange
        String email = "test@example.com";
        String password = "SecurePass123!";
        String salt = "randomSalt";
        String hashedPassword = SecurityUtils.hashPassword(password, salt);

        when(authCoreRepository.findByEmailAndStatus(email, AuthCore.AuthStatus.ACTIVE))
                .thenReturn(Optional.of(activeAuthCore));
        when(credentialsRepository.findByAuthCoreId(1L))
                .thenReturn(Optional.of(new AuthCredDB(hashedPassword, salt)));
        doNothing().when(sessionService).storeSession(anyString(), anyString());
        when(refreshTokenService.generateRefreshToken(anyString(), anyString()))
                .thenReturn("refresh-token-abc");

        // Act
        AuthenticationResult result = authService.authenticate(email, password, "127.0.0.1", "TestAgent");

        // Assert
        assertTrue(result.success());
        assertEquals("Authentication successful", result.message());
        assertNotNull(result.sessionId());
        assertEquals("refresh-token-abc", result.refreshToken());
        assertNotNull(result.authCore());
        verify(rateLimitingService).clearLoginAttempts(email, "127.0.0.1");
    }

    @Test
    @DisplayName("should return failure result when password is incorrect")
    void shouldReturnFailureWhenPasswordIsWrong() {
        // Arrange
        String email = "test@example.com";
        String wrongPassword = "WrongPass!";
        String salt = "randomSalt";
        String correctHash = SecurityUtils.hashPassword("CorrectPass123!", salt);

        when(authCoreRepository.findByEmailAndStatus(email, AuthCore.AuthStatus.ACTIVE))
                .thenReturn(Optional.of(activeAuthCore));
        when(credentialsRepository.findByAuthCoreId(1L))
                .thenReturn(Optional.of(new AuthCredDB(correctHash, salt)));

        // Act
        AuthenticationResult result = authService.authenticate(email, wrongPassword, "127.0.0.1", "TestAgent");

        // Assert
        assertFalse(result.success());
        assertEquals("INVALID_PASSWORD", result.code());
        assertNull(result.sessionId());
        assertNull(result.refreshToken());
        verify(rateLimitingService).recordFailedLogin(email, "127.0.0.1");
    }

    @Test
    @DisplayName("should clear failed login attempts on successful authentication")
    void shouldClearFailedAttemptsOnSuccess() {
        // Arrange
        String email = "test@example.com";
        String password = "SecurePass123!";
        String salt = "randomSalt";
        String hashedPassword = SecurityUtils.hashPassword(password, salt);

        when(authCoreRepository.findByEmailAndStatus(email, AuthCore.AuthStatus.ACTIVE))
                .thenReturn(Optional.of(activeAuthCore));
        when(credentialsRepository.findByAuthCoreId(1L))
                .thenReturn(Optional.of(new AuthCredDB(hashedPassword, salt)));
        doNothing().when(sessionService).storeSession(anyString(), anyString());
        when(refreshTokenService.generateRefreshToken(anyString(), anyString()))
                .thenReturn("refresh-token-xyz");

        // Act
        authService.authenticate(email, password, "192.168.1.1", "Chrome/120");

        // Assert
        verify(rateLimitingService).clearLoginAttempts(email, "192.168.1.1");
        verify(rateLimitingService, never()).recordFailedLogin(anyString(), anyString());
    }

    @Test
    @DisplayName("should return failure when user not found")
    void shouldReturnFailureWhenUserNotFound() {
        // Arrange
        when(authCoreRepository.findByEmailAndStatus("unknown@example.com", AuthCore.AuthStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(authCoreRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        // Act
        AuthenticationResult result = authService.authenticate("unknown@example.com", "pass", "127.0.0.1", "Agent");

        // Assert
        assertFalse(result.success());
        assertEquals("USER_NOT_FOUND", result.code());
    }
}
