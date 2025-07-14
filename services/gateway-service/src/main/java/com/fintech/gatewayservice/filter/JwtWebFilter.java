package com.fintech.gatewayservice.filter;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import com.fintech.gatewayservice.config.JwtConfig;
import com.fintech.gatewayservice.ratelimit.IpRateLimiterHandler;
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
        if (ipRateLimiterHandler.isAllowed(ip)) {
            exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS);
            return exchange.getResponse().setComplete();
        }

        if (path.startsWith("/api/auth")) {
            return chain.filter(exchange);
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        String jwt = authHeader.substring(7);
        boolean valid = jwtConfig.validateJwt(jwt);
        if (!valid) {
            exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }
}
