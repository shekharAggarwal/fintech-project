package com.fintech.gatewayservice.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.gatewayservice.config.JwtConfig;
import com.fintech.gatewayservice.config.RouteValidator;
import com.fintech.gatewayservice.external.model.response.AuthzIntrospectResponse;
import com.fintech.gatewayservice.external.service.AuthzService;
import com.fintech.gatewayservice.ratelimit.IpRateLimiterHandler;
import org.apache.hc.client5.http.utils.Base64;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class JwtAuthGatewayFilter extends AbstractGatewayFilterFactory<JwtAuthGatewayFilter.Config> {

    private final JwtConfig jwtConfig;
    private final IpRateLimiterHandler ipRateLimiterHandler;
    private final RouteValidator routeValidator;
    private final AuthzService authzService;
    private final ObjectMapper mapper = new ObjectMapper();

    public JwtAuthGatewayFilter(JwtConfig jwtConfig, IpRateLimiterHandler ipRateLimiterHandler, RouteValidator routeValidator, AuthzService authzService) {
        super(Config.class);
        this.jwtConfig = jwtConfig;
        this.ipRateLimiterHandler = ipRateLimiterHandler;
        this.routeValidator = routeValidator;
        this.authzService = authzService;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {

            ServerHttpRequest request = exchange.getRequest();
            String ip = request.getRemoteAddress() != null ?
                    request.getRemoteAddress().getAddress().getHostAddress() : "unknown";

            // 1️⃣ Per-IP rate limiting
            if (ipRateLimiterHandler.isRateLimited(ip)) {
                return deny(exchange, HttpStatus.TOO_MANY_REQUESTS, "IP rate limit exceeded", ip);
            }

            // 2️⃣ JWT validation for secured routes
            if (routeValidator.isSecured.test(request)) {
                String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    return deny(exchange, HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header", ip);
                }

                String jwt = authHeader.substring(7);
                if (!jwtConfig.validateJwt(jwt)) {
                    return deny(exchange, HttpStatus.UNAUTHORIZED, "Invalid JWT", ip);
                }


                // 3️⃣ Ask AuthZ service for decision (dynamic)
                return authzService.checkAccess(jwt, exchange.getRequest().getURI().getPath(),
                                exchange.getRequest().getMethod().name(),
                                Map.of("clientIp", ip))
                        .flatMap(authzResponse -> handleAuthzResponse(authzResponse, exchange, chain))
                        .onErrorResume(ex -> deny(exchange, HttpStatus.INTERNAL_SERVER_ERROR,
                                "Authorization service error", ip));
            }

            return chain.filter(exchange);

        });
    }

    private Mono<Void> handleAuthzResponse(AuthzIntrospectResponse resp, ServerWebExchange exchange, GatewayFilterChain chain) {
        if (resp == null || !resp.allowed) {
            return deny(exchange, HttpStatus.FORBIDDEN,
                    "Access denied: " + (resp != null ? resp.reason : "Invalid response"),
                    exchange.getRequest().getRemoteAddress() != null ?
                            exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown");
        }

        try {
            String json = mapper.writeValueAsString(resp);
            String base64 = Base64.encodeBase64String(json.getBytes(StandardCharsets.UTF_8));

            // add internal headers (trusted internal channel only)
            ServerWebExchange mutated = exchange.mutate()
                    .request(r -> r
                            .header("X-Authz", base64)
                            .header("X-User-Id", resp.userId == null ? "" : resp.userId)
                            .header("X-Role", resp.role == null || resp.role.isEmpty() ? "" : resp.role)
                    ).build();

            return chain.filter(mutated);
        } catch (Exception ex) {
            return deny(exchange, HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to process authorization response",
                    exchange.getRequest().getRemoteAddress() != null ?
                            exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown");
        }
    }

    static public class Config {
        private boolean required = true;

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean r) {
            this.required = r;
        }
    }


    private Mono<Void> deny(ServerWebExchange exchange, HttpStatus status, String reason, String ip) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("X-RateLimit-Reason", reason);
        exchange.getResponse().getHeaders().add("X-Frame-Options", "DENY");
        exchange.getResponse().getHeaders().add("X-XSS-Protection", "1; mode=block");
        exchange.getResponse().getHeaders().add("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        System.out.println(reason + " from IP: " + ip);
        return exchange.getResponse().setComplete();
    }

}
