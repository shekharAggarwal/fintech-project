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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SessionService {

    private static final Logger logger = LoggerFactory.getLogger(SessionService.class);
    private static final String SESSION_PREFIX = "session:";
    private static final String USER_SESSION_PREFIX = "user_sessions:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${security.session.redis-expiry}")
    private int sessionRedisExpiry;

    public SessionService(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Store session data in Redis
     */
    public void storeSession(String sessionId, String userId) {
        try {

            // Create session data
            Map<String, Object> sessionData = new HashMap<>();
            sessionData.put("userId", userId);
            sessionData.put("sessionId", sessionId);
            sessionData.put("createdAt", System.currentTimeMillis());

            String sessionJson = objectMapper.writeValueAsString(sessionData);

            // Store session with sessionId as key
            String sessionKey = SESSION_PREFIX + sessionId;
            redisTemplate.opsForValue().set(sessionKey, sessionJson, Duration.ofMillis(sessionRedisExpiry));

            // Track this session in the user's session set for bulk invalidation
            String userSessionsKey = USER_SESSION_PREFIX + userId;
            redisTemplate.opsForSet().add(userSessionsKey, sessionId);
            redisTemplate.expire(userSessionsKey, Duration.ofMillis(sessionRedisExpiry));

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
     * Retrieve userId from session stored in Redis.
     */
    public String getUserIdFromSession(String sessionId) {
        try {
            String sessionKey = SESSION_PREFIX + sessionId;
            String sessionJson = redisTemplate.opsForValue().get(sessionKey);

            if (sessionJson == null) {
                logger.warn("Session not found in Redis: sessionId={}", sessionId);
                return null;
            }

            Map<String, Object> sessionData = objectMapper.readValue(sessionJson, new TypeReference<>() {});
            return (String) sessionData.get("userId");

        } catch (Exception e) {
            logger.error("Failed to retrieve session from Redis: sessionId={}", sessionId, e);
            return null;
        }
    }

    /**
     * Invalidate (delete) a session from Redis.
     */
    public void invalidateSession(String sessionId) {
        try {
            String sessionKey = SESSION_PREFIX + sessionId;

            // Retrieve userId before deleting so we can clean up the user sessions set
            String sessionJson = redisTemplate.opsForValue().get(sessionKey);
            if (sessionJson != null) {
                Map<String, Object> sessionData = objectMapper.readValue(sessionJson, new TypeReference<>() {});
                String userId = (String) sessionData.get("userId");
                if (userId != null) {
                    String userSessionsKey = USER_SESSION_PREFIX + userId;
                    redisTemplate.opsForSet().remove(userSessionsKey, sessionId);
                }
            }

            Boolean deleted = redisTemplate.delete(sessionKey);
            if (Boolean.TRUE.equals(deleted)) {
                logger.info("Session invalidated in Redis: sessionId={}", sessionId);
            } else {
                logger.warn("Session not found for invalidation: sessionId={}", sessionId);
            }
        } catch (Exception e) {
            logger.error("Failed to invalidate session in Redis: sessionId={}", sessionId, e);
        }
    }

    /**
     * Invalidate all sessions for a given user.
     * Returns the list of invalidated session IDs so callers can blacklist associated tokens.
     */
    public List<String> invalidateAllUserSessions(String userId) {
        try {
            String userSessionsKey = USER_SESSION_PREFIX + userId;
            Set<String> sessionIds = redisTemplate.opsForSet().members(userSessionsKey);

            if (sessionIds == null || sessionIds.isEmpty()) {
                logger.info("No active sessions found for userId={}", userId);
                return Collections.emptyList();
            }

            // Delete each session key from Redis
            for (String sessionId : sessionIds) {
                String sessionKey = SESSION_PREFIX + sessionId;
                redisTemplate.delete(sessionKey);
            }

            // Remove the user sessions tracking set
            redisTemplate.delete(userSessionsKey);

            List<String> invalidatedIds = List.copyOf(sessionIds);
            logger.info("Invalidated {} sessions for userId={}", invalidatedIds.size(), userId);
            return invalidatedIds;

        } catch (Exception e) {
            logger.error("Failed to invalidate all sessions for userId={}", userId, e);
            // Fail secure: return empty list but don't suppress — caller should still revoke tokens
            return Collections.emptyList();
        }
    }

}
