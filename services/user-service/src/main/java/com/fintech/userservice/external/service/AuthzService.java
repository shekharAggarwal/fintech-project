package com.fintech.userservice.external.service;

import com.fintech.userservice.external.client.AuthzClient;
import com.fintech.userservice.external.model.request.UpdateRoleRequest;
import com.fintech.userservice.external.model.response.UpdateRoleResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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
    public Mono<UpdateRoleResponse> updateRole(String userId, String newRole, String updatedBy) {
        UpdateRoleRequest req = new UpdateRoleRequest(userId, newRole, updatedBy, "user-service");
        return authzClient.updateUserRole(req)
                .doOnSuccess(resp -> logger.info("✅ AuthZ success for for userId: {} - newRole: {} - updatedBy: {}", userId, newRole, updatedBy))
                .doOnError(ex -> logger.error("❌ AuthZ error for userId: {} - newRole: {} - updatedBy: {} - Error Type: {} - Message: {}",
                        userId, newRole, updatedBy, ex.getClass().getSimpleName(), ex.getMessage()));
    }

    // Fallback method - must match the signature of the main method exactly
    public Mono<UpdateRoleResponse> fallback(String userId, String newRole, String updatedBy, Exception ex) {
        logger.warn("🔴 CIRCUIT BREAKER FALLBACK TRIGGERED for userId: {} - newRole: {} - updatedBy: {} - Error: {} ", userId, newRole, updatedBy, ex.getMessage());

        UpdateRoleResponse deniedResponse = new UpdateRoleResponse();

        return Mono.just(deniedResponse);
    }
}
