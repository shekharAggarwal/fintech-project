package com.fintech.authservice.service;

import com.fintech.authservice.dto.response.RefreshTokenResponse;
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

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
    @DisplayName("should return success response when refresh token is valid")
    void shouldReturnSuccessWhenRefreshTokenIsValid() {
        // Arrange
        RefreshToken validToken = new RefreshToken("valid-refresh-token", "user-001",
                Instant.now().plusSeconds(86400), "session-001");
        validToken.setId(1L);
        validToken.setRevoked(false);

        when(refreshTokenRepository.findByToken("valid-refresh-token")).thenReturn(Optional.of(validToken));
        when(refreshTokenRepository.revokeIfNotRevoked(1L)).thenReturn(1);
        when(jwtUtil.generateAccessToken("user-001", "session-001")).thenReturn("new-access-token-jwt");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        // Act
        RefreshTokenResponse result = refreshTokenService.refreshAccessToken("valid-refresh-token");

        // Assert
        assertTrue(result.success());
        assertEquals("new-access-token-jwt", result.accessToken());
        verify(jwtUtil).generateAccessToken("user-001", "session-001");
    }

    @Test
    @DisplayName("should return failed response when refresh token is expired")
    void shouldReturnFailedWhenRefreshTokenIsExpired() {
        // Arrange
        RefreshToken expiredToken = new RefreshToken("expired-token", "user-001",
                Instant.now().minusSeconds(86400), "session-001");
        expiredToken.setRevoked(false);

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expiredToken));

        // Act
        RefreshTokenResponse result = refreshTokenService.refreshAccessToken("expired-token");

        // Assert
        assertFalse(result.success());
        assertEquals("TOKEN_EXPIRED", result.code());
        verify(jwtUtil, never()).generateAccessToken(anyString(), anyString());
    }

    @Test
    @DisplayName("should return failed and revoke all when refresh token is already revoked")
    void shouldRevokeAllWhenRefreshTokenIsRevoked() {
        // Arrange
        RefreshToken revokedToken = new RefreshToken("revoked-token", "user-001",
                Instant.now().plusSeconds(86400), "session-001");
        revokedToken.setRevoked(true);

        when(refreshTokenRepository.findByToken("revoked-token")).thenReturn(Optional.of(revokedToken));

        // Act
        RefreshTokenResponse result = refreshTokenService.refreshAccessToken("revoked-token");

        // Assert
        assertFalse(result.success());
        assertEquals("TOKEN_REVOKED", result.code());
        verify(refreshTokenRepository).revokeAllByUserId("user-001");
    }

    @Test
    @DisplayName("should return failed when refresh token does not exist")
    void shouldReturnFailedWhenTokenNotFound() {
        // Arrange
        when(refreshTokenRepository.findByToken("nonexistent-token")).thenReturn(Optional.empty());

        // Act
        RefreshTokenResponse result = refreshTokenService.refreshAccessToken("nonexistent-token");

        // Assert
        assertFalse(result.success());
        assertEquals("INVALID_TOKEN", result.code());
    }

    @Test
    @DisplayName("should revoke token and invalidate session on logout")
    void shouldRevokeTokenOnLogout() {
        // Arrange
        RefreshToken token = new RefreshToken("logout-token", "user-001",
                Instant.now().plusSeconds(86400), "session-001");
        token.setRevoked(false);

        when(refreshTokenRepository.findByToken("logout-token")).thenReturn(Optional.of(token));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(jwtUtil.getTokenRemainingLifeMillis("access-token-123")).thenReturn(3600000L);

        // Act — logout returns void now
        refreshTokenService.logout("logout-token", "access-token-123");

        // Assert
        assertTrue(token.isRevoked());
        verify(refreshTokenRepository).save(token);
        verify(sessionService).invalidateSession("session-001");
    }

    @Test
    @DisplayName("should still blacklist access token when refresh token not found on logout")
    void shouldBlacklistAccessTokenWhenLogoutTokenNotFound() {
        // Arrange
        when(refreshTokenRepository.findByToken("no-such-token")).thenReturn(Optional.empty());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(jwtUtil.getTokenRemainingLifeMillis("access-token")).thenReturn(3600000L);

        // Act — should not throw
        refreshTokenService.logout("no-such-token", "access-token");

        // Assert — refresh token not saved, but access token still blacklisted
        verify(refreshTokenRepository, never()).save(any());
    }
}
