package com.fintech.gatewayservice.ratelimit;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class IpRateLimiterHandler {
    private final RedissonClient redissonClient;
    private final RateLimiterConfig rateLimiterConfig;

    public IpRateLimiterHandler(RedissonClient redissonClient, RateLimiterConfig rateLimiterConfig) {
        this.redissonClient = redissonClient;
        this.rateLimiterConfig = rateLimiterConfig;
    }

    /**
     * Distributed rate limiting per IP using Redis counters and one config.
     *
     * @param ip Client IP
     * @return true if allowed, false if rate limited
     */
    public boolean isAllowed(String ip) {
        int limit = rateLimiterConfig.getLimitForPeriod();
        long periodSeconds = rateLimiterConfig.getLimitRefreshPeriod().getSeconds();
        String key = String.format("rate:%s", ip);
        long count = redissonClient.getAtomicLong(key).incrementAndGet();
        if (count == 1) {
            redissonClient.getAtomicLong(key).expire(Duration.ofSeconds(periodSeconds));
        }
        return count > limit;
    }

}
