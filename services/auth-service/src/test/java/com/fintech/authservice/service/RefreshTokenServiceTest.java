package com.fintech.authservice.service;

import com.fintech.authservice.entity.RefreshToken;
import com.fintech.authservice.repository.RefreshTokenRepository;
import com.fintech.authservice.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private SessionService sessionService;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("should return new access token when refresh token is valid")
    void shouldReturnNewAccessTokenWhenRefreshTokenIsValid() {
        // Arrange
        RefreshToken validToken = new RefreshToken("valid-refresh-token", "user-001", "session-001",
                LocalDateTime.now().plusDays(7));
        validToken.setRevoked(false);

        when(refreshTokenRepository.findByToken("valid-refresh-token")).thenReturn(Optional.of(validToken));
        when(jwtUtil.generateAccessToken("session-001")).thenReturn("new-access-token-jwt");

        // Act
        Optional<String> result = refreshTokenService.refreshAccessToken("valid-refresh-token");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("new-access-token-jwt", result.get());
        verify(jwtUtil).generateAccessToken("session-001");
    }

    @Test
    @DisplayName("should return empty when refresh token is expired")
    void shouldReturnEmptyWhenRefreshTokenIsExpired() {
        // Arrange
        RefreshToken expiredToken = new RefreshToken("expired-token", "user-001", "session-001",
                LocalDateTime.now().minusDays(1)); // Already expired
        expiredToken.setRevoked(false);

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

        // Act
        Optional<String> result = refreshTokenService.refreshAccessToken("expired-token");

        // Assert
        assertTrue(result.isEmpty());
        verify(jwtUtil, never()).generateAccessToken(anyString());
    }

    @Test
    @DisplayName("should return empty when refresh token is revoked")
    void shouldReturnEmptyWhenRefreshTokenIsRevoked() {
        // Arrange
        RefreshToken revokedToken = new RefreshToken("revoked-token", "user-001", "session-001",
                LocalDateTime.now().plusDays(7));
        revokedToken.setRevoked(true); // Revoked

        when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(revokedToken));

        // Act
        Optional<String> result = refreshTokenService.refreshAccessToken("revoked-token");

        // Assert
        assertTrue(result.isEmpty());
        verify(jwtUtil, never()).generateAccessToken(anyString());
    }

    @Test
    @DisplayName("should return empty when refresh token does not exist")
    void shouldReturnEmptyWhenTokenNotFound() {
        // Arrange
        when(refreshTokenRepository.findByToken("nonexistent-token")).thenReturn(Optional.empty());

        // Act
        Optional<String> result = refreshTokenService.refreshAccessToken("nonexistent-token");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("should revoke token and invalidate session on logout")
    void shouldRevokeTokenOnLogout() {
        // Arrange
        RefreshToken token = new RefreshToken("logout-token", "user-001", "session-001",
                LocalDateTime.now().plusDays(7));
        token.setRevoked(false);

        when(refreshTokenRepository.findByToken("logout-token")).thenReturn(Optional.of(token));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(redisTemplate.delete(anyString())).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(jwtUtil.getTokenRemainingLifeMillis("access-token-123")).thenReturn(3600000L);

        // Act
        boolean result = refreshTokenService.logout("logout-token", "access-token-123");

        // Assert
        assertTrue(result);
        assertTrue(token.getRevoked());
        verify(refreshTokenRepository).save(token);
        verify(redisTemplate).delete("session:session-001");
    }

    @Test
    @DisplayName("should return false when logout with non-existent refresh token")
    void shouldReturnFalseWhenLogoutTokenNotFound() {
        // Arrange
        when(refreshTokenRepository.findByToken("no-such-token")).thenReturn(Optional.empty());

        // Act
        boolean result = refreshTokenService.logout("no-such-token", "access-token");

        // Assert
        assertFalse(result);
        verify(refreshTokenRepository, never()).save(any());
    }
}
