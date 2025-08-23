package com.fintech.authservice.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Advanced rate limiting service for production-level security
 */
@Service
public class RateLimitingService {

    final private RedisTemplate<String, String> redisTemplate;

    // Login attempt limits
    private static final int MAX_LOGIN_ATTEMPTS_PER_EMAIL = 5;
    private static final int MAX_LOGIN_ATTEMPTS_PER_IP = 20;
    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(15);

    public RateLimitingService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Check if login is allowed based on email and IP rate limiting
     */
    public boolean isLoginAllowed(String email, String ipAddress) {
        return isEmailLoginAllowed(email) && isIpLoginAllowed(ipAddress);
    }

    /**
     * Check rate limiting for email-based login attempts
     */
    private boolean isEmailLoginAllowed(String email) {
        String key = "login_attempts:email:" + email;
        return checkRateLimit(key, MAX_LOGIN_ATTEMPTS_PER_EMAIL, LOGIN_WINDOW);
    }

    /**
     * Check rate limiting for IP-based login attempts
     */
    private boolean isIpLoginAllowed(String ipAddress) {
        String key = "login_attempts:ip:" + ipAddress;
        return checkRateLimit(key, MAX_LOGIN_ATTEMPTS_PER_IP, LOGIN_WINDOW);
    }

    /**
     * Record a failed login attempt
     */
    public void recordFailedLogin(String email, String ipAddress) {
        recordAttempt("login_attempts:email:" + email, LOGIN_WINDOW);
        recordAttempt("login_attempts:ip:" + ipAddress, LOGIN_WINDOW);
    }

    /**
     * Clear rate limiting after successful login
     */
    public void clearLoginAttempts(String email, String ipAddress) {
        redisTemplate.delete("login_attempts:email:" + email);
        // Don't clear IP attempts to prevent abuse
    }

    /**
     * Generic rate limiting check
     */
    private boolean checkRateLimit(String key, int maxAttempts, Duration window) {
        try {
            String currentCount = redisTemplate.opsForValue().get(key);

            if (currentCount == null) {
                return true; // No attempts recorded yet
            }

            int attempts = Integer.parseInt(currentCount);
            return attempts < maxAttempts;

        } catch (Exception e) {
            // If Redis is down, allow the request (fail open)
            return true;
        }
    }

    /**
     * Record an attempt with expiration
     */
    private void recordAttempt(String key, Duration window) {
        try {
            Long newCount = redisTemplate.opsForValue().increment(key);
            if (newCount == 1) {
                // Set expiration only on first attempt
                redisTemplate.expire(key, window.toSeconds(), TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            // Log error but don't fail the authentication process
        }
    }

    /**
     * Check if specific action is rate limited
     */
    public boolean isActionAllowed(String userId, String action, int maxAttempts, Duration window) {
        String key = String.format("action_limit:%s:%s", action, userId);
        return checkRateLimit(key, maxAttempts, window);
    }

    /**
     * Record an action attempt
     */
    public void recordAction(String userId, String action, Duration window) {
        String key = String.format("action_limit:%s:%s", action, userId);
        recordAttempt(key, window);
    }
}
