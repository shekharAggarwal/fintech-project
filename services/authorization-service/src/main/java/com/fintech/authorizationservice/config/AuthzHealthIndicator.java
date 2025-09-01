package com.fintech.authorizationservice.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
public class AuthzHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;
    private final RedisTemplate<String, String> redisTemplate;

    public AuthzHealthIndicator(DataSource dataSource, RedisTemplate<String, String> redisTemplate) {
        this.dataSource = dataSource;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Health health() {
        try {
            // Check database connection
            try (Connection connection = dataSource.getConnection()) {
                if (!connection.isValid(5)) {
                    return Health.down().withDetail("database", "Connection invalid").build();
                }
            }

            // Check Redis connection
            try {
                redisTemplate.opsForValue().get("health-check");
            } catch (Exception e) {
                return Health.down().withDetail("redis", "Connection failed: " + e.getMessage()).build();
            }

            return Health.up()
                    .withDetail("database", "Connected")
                    .withDetail("redis", "Connected")
                    .build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
