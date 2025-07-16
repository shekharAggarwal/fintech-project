package com.fintech.authorizationservice.service;

import com.fintech.authorizationservice.dto.AuthorizationRequest;
import com.fintech.authorizationservice.dto.AuthorizationResponse;
import com.fintech.authorizationservice.entity.Role;
import com.fintech.authorizationservice.entity.UserSession;
import com.fintech.authorizationservice.repository.RoleRepository;
import com.fintech.authorizationservice.repository.UserSessionRepository;
import com.fintech.authorizationservice.util.JwtUtil;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.micrometer.core.annotation.Timed;
import io.micrometer.tracing.annotation.NewSpan;
import io.micrometer.tracing.annotation.SpanTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class AuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationService.class);

    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private UserSessionRepository userSessionRepository;

    @NewSpan("authorization-check")
    @Timed(value = "authorization.duration", description = "Time taken for authorization check")
    @CircuitBreaker(name = "redisService", fallbackMethod = "authorizeFallback")
    @Retry(name = "redisService")
    public AuthorizationResponse authorize(@SpanTag("request") AuthorizationRequest request) {
        logger.debug("Authorizing request for resource: {} action: {}", request.getResource(), request.getAction());
        
        try {
            // Validate token first
            if (!jwtUtil.validateToken(request.getToken())) {
                logger.warn("Invalid or expired token provided");
                return new AuthorizationResponse(false, "Invalid or expired token");
            }

            // Extract session ID from token
            String sessionId = jwtUtil.getSessionIdFromToken(request.getToken());
            if (sessionId == null) {
                logger.warn("Session ID not found in token");
                return new AuthorizationResponse(false, "Session ID not found in token");
            }

            // Get user session from Redis (CAP theorem - Consistency + Partition tolerance)
            Optional<UserSession> sessionOpt = userSessionRepository.findById(sessionId);
            if (sessionOpt.isEmpty()) {
                logger.warn("Session not found or expired for sessionId: {}", sessionId);
                return new AuthorizationResponse(false, "Session not found or expired");
            }

            UserSession session = sessionOpt.get();
            session.updateLastAccessed(); // Update last accessed time
            userSessionRepository.save(session); // Save back to Redis

            // Check if user has required permission for the resource and action
            String requiredPermission = buildPermissionKey(request.getResource(), request.getAction());
            
            boolean hasPermission = session.getPermissions().contains(requiredPermission);
            
            if (hasPermission) {
                logger.info("Access granted for user: {} to resource: {} action: {}", 
                           session.getUserEmail(), request.getResource(), request.getAction());
                return new AuthorizationResponse(true, "Access granted", 
                        session.getRoleName(), session.getUserEmail());
            } else {
                logger.warn("Access denied for user: {} to resource: {} action: {}", 
                           session.getUserEmail(), request.getResource(), request.getAction());
                return new AuthorizationResponse(false, 
                    String.format("Access denied. User does not have permission: %s", requiredPermission));
            }

        } catch (Exception e) {
            logger.error("Authorization failed: ", e);
            return new AuthorizationResponse(false, "Authorization failed: " + e.getMessage());
        }
    }

    // Fallback method for circuit breaker
    public AuthorizationResponse authorizeFallback(AuthorizationRequest request, Exception ex) {
        logger.error("Authorization fallback triggered for resource: {} action: {}", 
                    request.getResource(), request.getAction(), ex);
        return new AuthorizationResponse(false, "Service temporarily unavailable. Please try again later.");
    }

    public AuthorizationResponse checkPermission(String token, String resource, String action) {
        AuthorizationRequest request = new AuthorizationRequest(token, resource, action);
        return authorize(request);
    }

    public Set<String> getUserAuthorities(String token) {
        try {
            if (!jwtUtil.validateToken(token)) {
                return Set.of();
            }

            String sessionId = jwtUtil.getSessionIdFromToken(token);
            if (sessionId == null) {
                return Set.of();
            }

            Optional<UserSession> sessionOpt = userSessionRepository.findById(sessionId);
            if (sessionOpt.isEmpty()) {
                return Set.of();
            }

            UserSession session = sessionOpt.get();
            session.updateLastAccessed();
            userSessionRepository.save(session);
            
            return session.getPermissions();

        } catch (Exception e) {
            return Set.of();
        }
    }

    // Create user session when user logs in (called from auth service)
    public UserSession createUserSession(String sessionId, String userEmail, String roleName) {
        try {
            // Get role permissions from database
            Optional<Role> roleOpt = roleRepository.findByNameWithPermissions(roleName);
            if (roleOpt.isEmpty()) {
                throw new RuntimeException("Role not found: " + roleName);
            }

            Role role = roleOpt.get();
            Set<String> permissions = role.getPermissions().stream()
                    .map(rp -> buildPermissionKey(rp.getPermission().getResource(), rp.getPermission().getAction()))
                    .collect(Collectors.toSet());

            // Create session in Redis
            UserSession session = new UserSession(sessionId, userEmail, roleName, permissions);
            
            // Remove any existing session for this user (single session enforcement)
            userSessionRepository.deleteByUserEmail(userEmail);
            
            return userSessionRepository.save(session);
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to create user session", e);
        }
    }

    public void invalidateUserSession(String sessionId) {
        userSessionRepository.deleteById(sessionId);
    }

    public void invalidateUserSessionsByEmail(String userEmail) {
        userSessionRepository.deleteByUserEmail(userEmail);
    }

    private String buildPermissionKey(String resource, String action) {
        if (action == null || action.isEmpty() || "view".equals(action)) {
            return resource.toUpperCase();
        }
        return resource.toUpperCase() + "_" + action.toUpperCase();
    }
}
