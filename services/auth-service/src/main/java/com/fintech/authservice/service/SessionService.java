package com.fintech.authservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class SessionService {

    private static final Logger logger = LoggerFactory.getLogger(SessionService.class);
    private static final String SESSION_PREFIX = "session:";
    private static final String USER_SESSION_PREFIX = "user_sessions:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${session.expiry.hours:24}")
    private int sessionExpiryHours;

    public SessionService(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Store session data in Redis
     */
    public void storeSession(String sessionId, String userId, String ipAddress, String userAgent) {
        try {
            long expiryTime = System.currentTimeMillis() + (sessionExpiryHours * 60 * 60 * 1000L);
            
            // Create session data
            Map<String, Object> sessionData = new HashMap<>();
            sessionData.put("userId", userId);
            sessionData.put("sessionId", sessionId);
            sessionData.put("expiryTime", expiryTime);
            sessionData.put("ipAddress", ipAddress);
            sessionData.put("userAgent", userAgent);
            sessionData.put("createdAt", System.currentTimeMillis());

            String sessionJson = objectMapper.writeValueAsString(sessionData);
            
            // Store session with sessionId as key
            String sessionKey = SESSION_PREFIX + sessionId;
            redisTemplate.opsForValue().set(sessionKey, sessionJson, Duration.ofHours(sessionExpiryHours));
            
            // Store userId to sessionId mapping for quick lookup
            String userSessionKey = USER_SESSION_PREFIX + userId;
            redisTemplate.opsForSet().add(userSessionKey, sessionId);
            redisTemplate.expire(userSessionKey, sessionExpiryHours, TimeUnit.HOURS);
            
            logger.info("Session stored in Redis: sessionId={}, userId={}", sessionId, userId);
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize session data for sessionId: {}, userId: {}", sessionId, userId, e);
            throw new RuntimeException("Failed to store session", e);
        } catch (Exception e) {
            logger.error("Failed to store session in Redis: sessionId={}, userId={}", sessionId, userId, e);
            throw new RuntimeException("Failed to store session", e);
        }
    }

    /**
     * Retrieve session data from Redis
     */
    public Map<String, Object> getSession(String sessionId) {
        try {
            String sessionKey = SESSION_PREFIX + sessionId;
            String sessionJson = redisTemplate.opsForValue().get(sessionKey);
            
            if (sessionJson != null) {
                TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {};
                Map<String, Object> sessionData = objectMapper.readValue(sessionJson, typeRef);
                
                // Check if session is expired
                Long expiryTime = ((Number) sessionData.get("expiryTime")).longValue();
                if (System.currentTimeMillis() > expiryTime) {
                    invalidateSession(sessionId);
                    return null;
                }
                
                return sessionData;
            }
            
            return null;
            
        } catch (Exception e) {
            logger.error("Failed to retrieve session from Redis: sessionId={}", sessionId, e);
            return null;
        }
    }

    /**
     * Invalidate session in Redis
     */
    public void invalidateSession(String sessionId) {
        try {
            // Get session data first to get userId
            Map<String, Object> sessionData = getSession(sessionId);
            
            String sessionKey = SESSION_PREFIX + sessionId;
            redisTemplate.delete(sessionKey);
            
            if (sessionData != null) {
                String userId = (String) sessionData.get("userId");
                String userSessionKey = USER_SESSION_PREFIX + userId;
                redisTemplate.opsForSet().remove(userSessionKey, sessionId);
            }
            
            logger.info("Session invalidated: sessionId={}", sessionId);
            
        } catch (Exception e) {
            logger.error("Failed to invalidate session: sessionId={}", sessionId, e);
        }
    }

    /**
     * Check if session is valid
     */
    public boolean isSessionValid(String sessionId) {
        Map<String, Object> sessionData = getSession(sessionId);
        return sessionData != null;
    }

    /**
     * Get session expiry time in milliseconds
     */
    public long getSessionExpiryTime() {
        return System.currentTimeMillis() + (sessionExpiryHours * 60 * 60 * 1000L);
    }
}
