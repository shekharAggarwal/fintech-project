package com.fintech.authservice.service;

import com.fintech.authservice.dto.response.RefreshTokenResponse;
import com.fintech.authservice.entity.RefreshToken;
import com.fintech.authservice.repository.RefreshTokenRepository;
import com.fintech.authservice.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class RefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final String BLACKLIST_PREFIX = "token_blacklist:";

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final SessionService sessionService;
    private final RedisTemplate<String, String> redisTemplate;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               JwtUtil jwtUtil,
                               SessionService sessionService,
                               RedisTemplate<String, String> redisTemplate) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtUtil = jwtUtil;
        this.sessionService = sessionService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Generate a new opaque refresh token and persist it.
     */
    public String generateRefreshToken(String userId, String sessionId) {
        String token = UUID.randomUUID().toString();
        Instant expiryDate = Instant.now().plusMillis(refreshTokenExpiration);

        RefreshToken refreshToken = new RefreshToken(token, userId, expiryDate, sessionId);
        refreshTokenRepository.save(refreshToken);

        logger.info("Generated refresh token for userId={}, sessionId={}", userId, sessionId);
        return token;
    }

    /**
     * Validate the refresh token and issue a new access token.
     * Implements refresh token rotation — old token is revoked, new one issued.
     */
    public RefreshTokenResponse refreshAccessToken(String refreshToken) {
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByToken(refreshToken);

        if (tokenOpt.isEmpty()) {
            logger.warn("Refresh token not found: attempted token reuse or invalid token");
            return RefreshTokenResponse.failed("Invalid refresh token", "INVALID_TOKEN");
        }

        RefreshToken storedToken = tokenOpt.get();

        if (storedToken.isRevoked()) {
            // Possible token theft — revoke all tokens for this user
            logger.warn("Revoked refresh token used for userId={}. Possible token theft — revoking all tokens.", storedToken.getUserId());
            refreshTokenRepository.revokeAllByUserId(storedToken.getUserId());
            return RefreshTokenResponse.failed("Token has been revoked. Please login again.", "TOKEN_REVOKED");
        }

        if (storedToken.isExpired()) {
            logger.info("Expired refresh token used for userId={}", storedToken.getUserId());
            return RefreshTokenResponse.failed("Refresh token expired. Please login again.", "TOKEN_EXPIRED");
        }

        // Rotate: revoke old token, issue new one
        storedToken.setRevoked(true);
        refreshTokenRepository.save(storedToken);

        // Generate new access token using the session
        String newAccessToken = jwtUtil.generateAccessToken(storedToken.getSessionId());

        // Generate new refresh token (rotation)
        String newRefreshToken = generateRefreshToken(storedToken.getUserId(), storedToken.getSessionId());

        logger.info("Access token refreshed for userId={}", storedToken.getUserId());
        return RefreshTokenResponse.success(newAccessToken, newRefreshToken);
    }

    /**
     * Logout: revoke the refresh token and blacklist the current access token in Redis.
     */
    public void logout(String refreshToken, String accessToken) {
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByToken(refreshToken);

        if (tokenOpt.isPresent()) {
            RefreshToken storedToken = tokenOpt.get();
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);

            // Invalidate the session in Redis
            if (storedToken.getSessionId() != null) {
                sessionService.invalidateSession(storedToken.getSessionId());
            }

            logger.info("Refresh token revoked for userId={}", storedToken.getUserId());
        }

        // Blacklist the access token for its remaining lifetime
        if (accessToken != null) {
            blacklistAccessToken(accessToken);
        }
    }

    /**
     * Logout from all devices: revoke all refresh tokens for the user.
     */
    public void logoutAllDevices(String userId) {
        int revokedCount = refreshTokenRepository.revokeAllByUserId(userId);
        logger.info("Revoked {} refresh tokens for userId={}", revokedCount, userId);
    }

    /**
     * Blacklist an access token in Redis so it cannot be used until it expires naturally.
     */
    private void blacklistAccessToken(String accessToken) {
        try {
            long remainingMillis = jwtUtil.getTokenRemainingLifeMillis(accessToken);
            if (remainingMillis > 0) {
                String blacklistKey = BLACKLIST_PREFIX + accessToken;
                redisTemplate.opsForValue().set(blacklistKey, "revoked", Duration.ofMillis(remainingMillis));
                logger.debug("Access token blacklisted for {}ms", remainingMillis);
            }
        } catch (Exception e) {
            logger.error("Failed to blacklist access token", e);
        }
    }

    /**
     * Check if an access token has been blacklisted (logged out).
     */
    public boolean isTokenBlacklisted(String accessToken) {
        try {
            String blacklistKey = BLACKLIST_PREFIX + accessToken;
            return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
        } catch (Exception e) {
            logger.error("Failed to check token blacklist", e);
            // Fail secure: if Redis is down, reject the token
            return true;
        }
    }
}
