package com.fintech.authservice.config;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RateLimitConfig {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    public RateLimitConfig(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    public boolean isAllowed(String key, RateLimitType type) {
        String redisKey = "rate_limit:" + type.name() + ":" + key;
        
        RateLimitSettings settings = getRateLimitSettings(type);
        
        // Get current count
        String countStr = (String) redisTemplate.opsForValue().get(redisKey);
        int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;
        
        if (currentCount >= settings.maxRequests) {
            return false;
        }
        
        // Increment counter
        redisTemplate.opsForValue().increment(redisKey);
        
        // Set expiration if this is the first request
        if (currentCount == 0) {
            redisTemplate.expire(redisKey, settings.windowDuration);
        }
        
        return true;
    }
    
    private RateLimitSettings getRateLimitSettings(RateLimitType type) {
        switch (type) {
            case LOGIN:
                return new RateLimitSettings(5, Duration.ofMinutes(1));
            case REGISTRATION:
                return new RateLimitSettings(3, Duration.ofHours(1));
            case PASSWORD_RESET:
                return new RateLimitSettings(2, Duration.ofHours(1));
            case GENERAL_API:
            default:
                return new RateLimitSettings(100, Duration.ofMinutes(1));
        }
    }
    
    public enum RateLimitType {
        LOGIN, REGISTRATION, PASSWORD_RESET, GENERAL_API
    }
    
    private static class RateLimitSettings {
        final int maxRequests;
        final Duration windowDuration;
        
        RateLimitSettings(int maxRequests, Duration windowDuration) {
            this.maxRequests = maxRequests;
            this.windowDuration = windowDuration;
        }
    }
}
