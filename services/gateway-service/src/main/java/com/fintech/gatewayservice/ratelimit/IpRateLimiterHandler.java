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
     * Returns true if the IP is rate limited, false if allowed.
     */
    public boolean isRateLimited(String ip) {
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
