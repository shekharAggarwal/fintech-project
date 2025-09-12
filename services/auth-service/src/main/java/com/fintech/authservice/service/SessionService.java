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

            logger.info("Session stored in Redis: sessionId={}, userId={}", sessionId, userId);

        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize session data for sessionId: {}, userId: {}", sessionId, userId, e);
            throw new RuntimeException("Failed to store session", e);
        } catch (Exception e) {
            logger.error("Failed to store session in Redis: sessionId={}, userId={}", sessionId, userId, e);
            throw new RuntimeException("Failed to store session", e);
        }
    }

}
