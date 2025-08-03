package com.fintech.gatewayservice.filter;

import com.fintech.gatewayservice.config.JwtConfig;
import com.fintech.gatewayservice.ratelimit.IpRateLimiterHandler;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class JwtWebFilter implements WebFilter {
    private final JwtConfig jwtConfig;
    private final IpRateLimiterHandler ipRateLimiterHandler;

    public JwtWebFilter(JwtConfig jwtConfig, IpRateLimiterHandler ipRateLimiterHandler) {
        this.jwtConfig = jwtConfig;
        this.ipRateLimiterHandler = ipRateLimiterHandler;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        String ip = request.getRemoteAddress() != null ? request.getRemoteAddress().getAddress().getHostAddress() : "unknown";

        // Per-IP rate limiting
        if (ipRateLimiterHandler.isRateLimited(ip)) {
            exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders().add("X-RateLimit-Reason", "IP rate limit exceeded");
            // Add security headers
            exchange.getResponse().getHeaders().add("X-Frame-Options", "DENY");
            exchange.getResponse().getHeaders().add("X-XSS-Protection", "1; mode=block");
            exchange.getResponse().getHeaders().add("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            System.out.println("Rate limited IP: " + ip);
            return exchange.getResponse().setComplete();
        }

        if (path.startsWith("/api/auth") || path.startsWith("/actuator")) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().add("X-Frame-Options", "DENY");
            exchange.getResponse().getHeaders().add("X-XSS-Protection", "1; mode=block");
            exchange.getResponse().getHeaders().add("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            System.out.println("Unauthorized access from IP: " + ip);
            return exchange.getResponse().setComplete();
        }
        String jwt = authHeader.substring(7);
        boolean valid = jwtConfig.validateJwt(jwt);
        if (!valid) {
            exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().add("X-Frame-Options", "DENY");
            exchange.getResponse().getHeaders().add("X-XSS-Protection", "1; mode=block");
            exchange.getResponse().getHeaders().add("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            System.out.println("Invalid JWT from IP: " + ip);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }
}
