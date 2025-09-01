package com.fintech.gatewayservice.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.gatewayservice.client.AuthzClient;
import com.fintech.gatewayservice.config.JwtConfig;
import com.fintech.gatewayservice.config.RouteValidator;
import com.fintech.gatewayservice.dto.request.AuthzIntrospectRequest;
import com.fintech.gatewayservice.dto.response.AuthzIntrospectResponse;
import com.fintech.gatewayservice.ratelimit.IpRateLimiterHandler;
import org.apache.hc.client5.http.utils.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthGatewayFilter.class);

    private final RouteValidator routeValidator;
    private final AuthzClient authzClient;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${gateway.authz.timeout-ms:1200}")
    private int timeoutMs;

    public JwtAuthGatewayFilter(JwtConfig jwtConfig, IpRateLimiterHandler ipRateLimiterHandler, RouteValidator routeValidator, AuthzClient authzClient) {
        super(Config.class);
        this.jwtConfig = jwtConfig;
        this.ipRateLimiterHandler = ipRateLimiterHandler;
        this.routeValidator = routeValidator;
        this.authzClient = authzClient;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {

            ServerHttpRequest request = exchange.getRequest();
            String ip = request.getRemoteAddress() != null ?
                    request.getRemoteAddress().getAddress().getHostAddress() : "unknown";

            // 1️⃣ Per-IP rate limiting
            if (ipRateLimiterHandler.isRateLimited(ip)) {
                return deny(exchange, org.springframework.http.HttpStatus.TOO_MANY_REQUESTS, "IP rate limit exceeded", ip);
            }

            // 2️⃣ JWT validation for secured routes
            if (routeValidator.isSecured.test(request)) {
                String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    return deny(exchange, org.springframework.http.HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header", ip);
                }

                String jwt = authHeader.substring(7);
                if (!jwtConfig.validateJwt(jwt)) {
                    return deny(exchange, org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid JWT", ip);
                }
                // 3️⃣ Ask AuthZ service for decision (dynamic)
                AuthzIntrospectRequest req = new AuthzIntrospectRequest();
                req.jwtToken = jwt;
                req.path = exchange.getRequest().getURI().getPath();
                req.method = exchange.getRequest().getMethod().name();
                req.context = Map.of("clientIp", ip);
                return authzClient.introspect(req, timeoutMs)
                        .flatMap(resp -> handleAuthzResponse(resp, exchange, chain))
                        .onErrorResume(ex -> {
                            logger.error("Authz introspect error: {}", ex.toString());
                            // Fail-closed for transaction endpoints or return 500 for others
                            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                            return exchange.getResponse().setComplete();
                        });
            }

            return chain.filter(exchange);
        });
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

    private Mono<Void> handleAuthzResponse(AuthzIntrospectResponse resp, ServerWebExchange exchange, GatewayFilterChain chain) {
        if (resp == null || !resp.allowed) {
            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
            return exchange.getResponse().setComplete();
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

            /*// convenience headers
            if (resp.fieldAccess != null && resp.fieldAccess.containsKey("user")) {
                mutated.getRequest().mutate()
                        .header("X-Allowed-Fields", String.join(",", resp.fieldAccess.get("user")));
            }

            if (resp.permissions != null) {
                Object perTxn = resp.permissions.get("perTxnMax");
                Object daily = resp.permissions.get("dailyMax");
                if (perTxn != null) mutated.getRequest().mutate().header("X-Txn-Limit-PerTxn", String.valueOf(perTxn));
                if (daily != null) mutated.getRequest().mutate().header("X-Txn-Limit-Daily", String.valueOf(daily));
            }*/

            return chain.filter(mutated);
        } catch (Exception ex) {
            logger.error("Failed to attach authz envelope: {}", ex.toString());
            exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return exchange.getResponse().setComplete();
        }
    }

    private Mono<Void> deny(ServerWebExchange exchange, org.springframework.http.HttpStatus status, String reason, String ip) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("X-RateLimit-Reason", reason);
        exchange.getResponse().getHeaders().add("X-Frame-Options", "DENY");
        exchange.getResponse().getHeaders().add("X-XSS-Protection", "1; mode=block");
        exchange.getResponse().getHeaders().add("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        System.out.println(reason + " from IP: " + ip);
        return exchange.getResponse().setComplete();
    }

}
