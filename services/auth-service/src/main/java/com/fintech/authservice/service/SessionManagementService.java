package com.fintech.authservice.service;

import com.fintech.authservice.entity.UserSession;
import com.fintech.authservice.repository.UserSessionRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Service for managing user sessions
 * Handles session creation, validation, and cleanup
 * Integrates with Splunk for centralized session monitoring
 */
@Service
@Transactional
public class SessionManagementService {

    private final UserSessionRepository userSessionRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SplunkLoggingService splunkLoggingService;
    
    private static final String SESSION_PREFIX = "session:";
    private static final String TRUSTED_DEVICE_PREFIX = "trusted_device:";

    public SessionManagementService(UserSessionRepository userSessionRepository,
                                   RedisTemplate<String, Object> redisTemplate,
                                   SplunkLoggingService splunkLoggingService) {
        this.userSessionRepository = userSessionRepository;
        this.redisTemplate = redisTemplate;
        this.splunkLoggingService = splunkLoggingService;
    }

    /**
     * Check if a device is trusted for a user
     */
    public boolean isTrustedDevice(String userId, String deviceFingerprint) {
        if (deviceFingerprint == null) return false;
        
        // Check in Redis cache first
        String cacheKey = TRUSTED_DEVICE_PREFIX + userId + ":" + deviceFingerprint;
        Boolean isTrusted = (Boolean) redisTemplate.opsForValue().get(cacheKey);
        
        if (isTrusted != null) {
            return isTrusted;
        }
        
        // Check in database
        List<UserSession> trustedSessions = userSessionRepository
            .findActiveSessionsByUserAndDevice(userId, deviceFingerprint);
        
        boolean trusted = trustedSessions.stream()
            .anyMatch(UserSession::getIsTrustedDevice);
        
        // Cache the result
        redisTemplate.opsForValue().set(cacheKey, trusted, 1, TimeUnit.HOURS);
        
        return trusted;
    }

    /**
     * Mark a device as trusted
     */
    public void markDeviceAsTrusted(String userId, String deviceFingerprint) {
        // Update all sessions for this device
        List<UserSession> sessions = userSessionRepository
            .findActiveSessionsByUserAndDevice(userId, deviceFingerprint);
        
        for (UserSession session : sessions) {
            session.setIsTrustedDevice(true);
            userSessionRepository.save(session);
        }
        
        // Update cache
        String cacheKey = TRUSTED_DEVICE_PREFIX + userId + ":" + deviceFingerprint;
        redisTemplate.opsForValue().set(cacheKey, true, 24, TimeUnit.HOURS);
        
        // Log to Splunk
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("device_fingerprint", deviceFingerprint);
        metadata.put("sessions_updated", sessions.size());
        
        splunkLoggingService.logSessionEvent(
            sessions.isEmpty() ? null : sessions.get(0).getSessionId(),
            userId,
            "DEVICE_MARKED_TRUSTED",
            sessions.isEmpty() ? null : sessions.get(0).getIpAddress(),
            deviceFingerprint,
            metadata
        );
    }

    /**
     * Revoke a specific session
     */
    public void revokeSession(String sessionId) {
        userSessionRepository.findBySessionId(sessionId)
            .ifPresent(session -> {
                session.revoke();
                userSessionRepository.save(session);
                
                // Remove from Redis
                redisTemplate.delete(SESSION_PREFIX + sessionId);
                
                // Log to Splunk
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("reason", "manual_revocation");
                
                splunkLoggingService.logSessionEvent(
                    sessionId,
                    session.getUserId(),
                    "SESSION_REVOKED",
                    session.getIpAddress(),
                    session.getDeviceFingerprint(),
                    metadata
                );
            });
    }

    /**
     * Revoke all sessions for a user
     */
    public void revokeAllUserSessions(String userId) {
        List<UserSession> sessions = userSessionRepository.findActiveSessionsByUserId(userId);
        
        for (UserSession session : sessions) {
            session.revoke();
            userSessionRepository.save(session);
            
            // Remove from Redis
            redisTemplate.delete(SESSION_PREFIX + session.getSessionId());
        }
    }

    /**
     * Enforce maximum concurrent sessions per user
     */
    public void enforceSessionLimit(String userId, int maxSessions) {
        List<UserSession> activeSessions = userSessionRepository
            .findActiveSessionsByUserId(userId);
        
        if (activeSessions.size() > maxSessions) {
            // Sort by last accessed time and revoke oldest sessions
            activeSessions.sort((s1, s2) -> s1.getLastAccessedAt().compareTo(s2.getLastAccessedAt()));
            
            int sessionsToRevoke = activeSessions.size() - maxSessions;
            for (int i = 0; i < sessionsToRevoke; i++) {
                UserSession session = activeSessions.get(i);
                session.revoke();
                userSessionRepository.save(session);
                
                // Remove from Redis
                redisTemplate.delete(SESSION_PREFIX + session.getSessionId());
            }
        }
    }

    /**
     * Get all active sessions for a user
     */
    public List<UserSession> getUserActiveSessions(String userId) {
        return userSessionRepository.findActiveSessionsByUserId(userId);
    }

    /**
     * Check if a session is valid
     */
    public boolean isSessionValid(String sessionId) {
        // Check Redis first for performance
        String userId = (String) redisTemplate.opsForValue().get(SESSION_PREFIX + sessionId);
        if (userId == null) {
            return false;
        }
        
        // Verify in database
        return userSessionRepository.findActiveSessionById(sessionId).isPresent();
    }

    /**
     * Update session last accessed time
     */
    public void updateSessionAccess(String sessionId) {
        userSessionRepository.findActiveSessionById(sessionId)
            .ifPresent(session -> {
                session.markAsAccessed();
                userSessionRepository.save(session);
            });
    }

    /**
     * Clean up expired sessions
     */
    @Transactional
    public void cleanupExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        List<UserSession> expiredSessions = userSessionRepository.findExpiredSessions(now);
        
        for (UserSession session : expiredSessions) {
            session.setStatus(UserSession.SessionStatus.EXPIRED);
            session.setTerminatedAt(now);
            userSessionRepository.save(session);
            
            // Remove from Redis
            redisTemplate.delete(SESSION_PREFIX + session.getSessionId());
            
            // Log to Splunk
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("cleanup_time", now.toString());
            metadata.put("session_duration_minutes", 
                java.time.Duration.between(session.getCreatedAt(), now).toMinutes());
            
            splunkLoggingService.logSessionEvent(
                session.getSessionId(),
                session.getUserId(),
                "SESSION_EXPIRED_CLEANUP",
                session.getIpAddress(),
                session.getDeviceFingerprint(),
                metadata
            );
        }
        
        // Log cleanup summary to Splunk
        if (!expiredSessions.isEmpty()) {
            Map<String, Object> summaryData = new HashMap<>();
            summaryData.put("sessions_cleaned", expiredSessions.size());
            summaryData.put("cleanup_timestamp", now.toString());
            
            splunkLoggingService.logPerformanceMetric(
                "session_cleanup",
                System.currentTimeMillis(),
                true,
                summaryData
            );
        }
    }

    /**
     * Get session statistics for monitoring
     */
    public SessionStats getSessionStats() {
        long totalActiveSessions = userSessionRepository.count();
        List<UserSession> suspiciousSessions = userSessionRepository.findSuspiciousSessions();
        
        return new SessionStats(totalActiveSessions, suspiciousSessions.size());
    }

    /**
     * Mark sessions as suspicious based on unusual activity
     */
    public void markSuspiciousSessions(String userId, String reason) {
        List<UserSession> sessions = userSessionRepository.findActiveSessionsByUserId(userId);
        
        for (UserSession session : sessions) {
            session.markAsSuspicious();
            userSessionRepository.save(session);
        }
    }

    /**
     * Session statistics data class
     */
    public static class SessionStats {
        private final long totalActiveSessions;
        private final long suspiciousSessions;

        public SessionStats(long totalActiveSessions, long suspiciousSessions) {
            this.totalActiveSessions = totalActiveSessions;
            this.suspiciousSessions = suspiciousSessions;
        }

        public long getTotalActiveSessions() { return totalActiveSessions; }
        public long getSuspiciousSessions() { return suspiciousSessions; }
    }
}
