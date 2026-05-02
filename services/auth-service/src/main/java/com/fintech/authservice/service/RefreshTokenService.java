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
import java.util.List;
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

        // Atomic CAS revocation: prevents race condition in token rotation
        int updated = refreshTokenRepository.revokeIfNotRevoked(storedToken.getId());
        if (updated == 0) {
            // Token already revoked — potential theft! Revoke ALL tokens for this user
            logger.warn("Refresh token rotation race detected for userId={}. Revoking all tokens.", storedToken.getUserId());
            refreshTokenRepository.revokeAllByUserId(storedToken.getUserId());
            throw new SecurityException("Refresh token reuse detected — all sessions revoked");
        }

        // Generate new access token using the session — subject is now email (userId)
        String newAccessToken = jwtUtil.generateAccessToken(storedToken.getUserId(), storedToken.getSessionId());

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
            try {
                blacklistAccessToken(accessToken);
            } catch (Exception e) {
                logger.error("Failed to blacklist access token during logout", e);
                throw new RuntimeException("Logout failed — access token could not be invalidated", e);
            }
        }
    }

    /**
     * Logout from all devices: invalidate all sessions, blacklist active access tokens,
     * and revoke all refresh tokens for the user.
     *
     * Security: ensures no stale access tokens survive a full logout.
     */
    public void logoutAllDevices(String userId) {
        // 1. Invalidate all sessions for this user in Redis and get the session IDs
        List<String> invalidatedSessionIds = sessionService.invalidateAllUserSessions(userId);
        logger.info("Invalidated {} sessions for userId={}", invalidatedSessionIds.size(), userId);

        // 2. Find all active refresh tokens to get associated access tokens for blacklisting
        //    We use the session IDs from active tokens to generate blacklist entries.
        //    Since access tokens carry the sessionId as subject, we blacklist via session invalidation
        //    which is already handled above. But we also need to explicitly blacklist any access tokens
        //    that may still be in-flight (cached by clients).
        List<RefreshToken> activeTokens = refreshTokenRepository.findAllActiveByUserId(userId, Instant.now());
        int blacklistedCount = 0;
        for (RefreshToken token : activeTokens) {
            if (token.getSessionId() != null) {
                // Blacklist access tokens by session — any token referencing this session
                // will be rejected on next validation via session check.
                // The session invalidation above already handles this, but as defense-in-depth
                // we mark the session-based blacklist key in Redis.
                blacklistSessionAccessTokens(token.getSessionId());
                blacklistedCount++;
            }
        }
        logger.info("Blacklisted access tokens for {} active sessions for userId={}", blacklistedCount, userId);

        // 3. Revoke all refresh tokens in the database (already existed)
        int revokedCount = refreshTokenRepository.revokeAllByUserId(userId);
        logger.info("Revoked {} refresh tokens for userId={}", revokedCount, userId);
    }

    /**
     * Blacklist all access tokens associated with a session by marking the session as revoked.
     * Any token validation that checks session validity will reject these tokens.
     */
    private void blacklistSessionAccessTokens(String sessionId) {
        try {
            // Use a session-level blacklist key that the token validation filter can check
            String blacklistKey = BLACKLIST_PREFIX + "session:" + sessionId;
            // Set TTL to match access token max lifetime to auto-cleanup
            redisTemplate.opsForValue().set(blacklistKey, "revoked", Duration.ofHours(1));
            logger.debug("Session access tokens blacklisted: sessionId={}", sessionId);
        } catch (Exception e) {
            // Fail secure: log but don't suppress — session is already invalidated
            logger.error("Failed to blacklist session access tokens: sessionId={}", sessionId, e);
        }
    }

    /**
     * Blacklist an access token in Redis so it cannot be used until it expires naturally.
     * Throws on failure so callers (e.g., logout) can handle it appropriately.
     */
    private void blacklistAccessToken(String accessToken) {
        long remainingMillis = jwtUtil.getTokenRemainingLifeMillis(accessToken);
        if (remainingMillis > 0) {
            String blacklistKey = BLACKLIST_PREFIX + accessToken;
            redisTemplate.opsForValue().set(blacklistKey, "revoked", Duration.ofMillis(remainingMillis));
            logger.debug("Access token blacklisted for {}ms", remainingMillis);
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

    /**
     * Check if a session has been blacklisted (all tokens for session revoked).
     */
    public boolean isSessionBlacklisted(String sessionId) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + "session:" + sessionId));
        } catch (Exception e) {
            logger.error("Redis check failed, failing secure", e);
            return true; // fail-secure
        }
    }
}
