package com.fintech.gatewayservice.external.service;

import com.fintech.gatewayservice.external.client.AuthzClient;
import com.fintech.gatewayservice.external.model.request.AuthzIntrospectRequest;
import com.fintech.gatewayservice.external.model.response.AuthzIntrospectResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class AuthzService {

    @Value("${gateway.authz.timeout-ms:3000}")
    private int timeoutMs;

    final Logger logger = LoggerFactory.getLogger(AuthzService.class);

    private final AuthzClient authzClient;
    
    public AuthzService(AuthzClient authzClient) {
        this.authzClient = authzClient;
    }

    @CircuitBreaker(name = "authzService", fallbackMethod = "fallback")
    public Mono<AuthzIntrospectResponse> checkAccess(String jwt, String path, String method, Map<String, Object> context) {
        logger.info("🔵 AuthzService.checkAccess called for path: {} with CircuitBreaker", path);
        
        AuthzIntrospectRequest req = new AuthzIntrospectRequest();
        req.jwtToken = jwt;
        req.path = path;
        req.method = method;
        req.context = context;

        return authzClient.introspect(req)
                .doOnSuccess(resp -> logger.info("✅ AuthZ success for path: {} - allowed: {}", path, resp.allowed))
                .doOnError(ex -> logger.error("❌ AuthZ error for path: {} - Error Type: {} - Message: {}", 
                          path, ex.getClass().getSimpleName(), ex.getMessage()));
    }

    // Fallback method - must match the signature of the main method exactly
    public Mono<AuthzIntrospectResponse> fallback(String jwt, String path, String method, Map<String, Object> context, Exception ex) {
        logger.warn("🔴 CIRCUIT BREAKER FALLBACK TRIGGERED for path: {} - Error: {}", path, ex.getMessage());
        
        AuthzIntrospectResponse deniedResponse = new AuthzIntrospectResponse();
        deniedResponse.allowed = false;
        deniedResponse.reason = "Authorization service unavailable - CircuitBreaker OPEN";
        
        return Mono.just(deniedResponse);
    }
}
