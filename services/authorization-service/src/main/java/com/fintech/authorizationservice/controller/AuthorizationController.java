package com.fintech.authorizationservice.controller;

import com.fintech.authorizationservice.dto.request.AuthzIntrospectRequest;
import com.fintech.authorizationservice.dto.request.UpdateUserRoleRequest;
import com.fintech.authorizationservice.dto.response.AuthzIntrospectResponse;
import com.fintech.authorizationservice.service.AuthzService;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/authz")
public class AuthorizationController {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationController.class);
    private final AuthzService authzService;
    private final Tracer tracer;

    public AuthorizationController(AuthzService authzService, Tracer tracer) {
        this.authzService = authzService;
        this.tracer = tracer;
    }

    @PostMapping("/introspect")
    public Mono<ResponseEntity<AuthzIntrospectResponse>> introspect(@RequestBody AuthzIntrospectRequest req) {
        return authzService.introspect(req).map(ResponseEntity::ok);
    }

    @PutMapping("/internal/user-role")
    public ResponseEntity<?> updateUserRole(@RequestBody UpdateUserRoleRequest request) {
        // Log current trace context when receiving request
        String traceId = tracer.currentSpan() != null ? tracer.currentSpan().context().traceId() : "no-trace";
        String spanId = tracer.currentSpan() != null ? tracer.currentSpan().context().spanId() : "no-span";

        logger.info("Received internal user role update request with trace [{}] span [{}] for userId={} newRole={} from service={}", traceId, spanId, request.getUserId(), request.getNewRole(), request.getServiceSource());

        try {
            // Validate request
            if (request.getUserId() == null || request.getUserId().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid userId", "reason", "userId cannot be empty"));
            }

            if (request.getNewRole() == null || request.getNewRole().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid role", "reason", "newRole cannot be empty"));
            }

            if (request.getServiceSource() == null || request.getServiceSource().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid serviceSource", "reason", "serviceSource must be provided"));
            }

            // Update the user role
            authzService.updateUserRole(request.getUserId(), request.getNewRole(), request.getUpdatedBy());

            logger.info("Successfully updated user role with trace [{}] for userId={} to role={} by={} from service={}", traceId, request.getUserId(), request.getNewRole(), request.getUpdatedBy(), request.getServiceSource());

            return ResponseEntity.ok(Map.of("message", "User role updated successfully", "userId", request.getUserId(), "newRole", request.getNewRole(), "updatedBy", request.getUpdatedBy(), "serviceSource", request.getServiceSource(), "timestamp", System.currentTimeMillis()));

        } catch (RuntimeException e) {
            logger.error("Error updating user role with trace [{}] for userId={} to role={}: {}", traceId, request.getUserId(), request.getNewRole(), e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", "Role update failed", "reason", e.getMessage()));
        }
    }
}
