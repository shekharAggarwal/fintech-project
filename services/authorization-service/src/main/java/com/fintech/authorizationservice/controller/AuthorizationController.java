package com.fintech.authorizationservice.controller;

import com.fintech.authorizationservice.dto.request.AuthzIntrospectRequest;
import com.fintech.authorizationservice.dto.response.AuthzIntrospectResponse;
import com.fintech.authorizationservice.service.AuthzService;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

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
        // Log current trace context when receiving request
        String traceId = tracer.currentSpan() != null ? 
            tracer.currentSpan().context().traceId() : "no-trace";
        String spanId = tracer.currentSpan() != null ?
            tracer.currentSpan().context().spanId() : "no-span";
            
        logger.info("Received authz introspect request with trace [{}] span [{}] for path={} method={}", 
            traceId, spanId, req.path(), req.method());
            
        return authzService.introspect(req)
                .doOnSuccess(response -> {
                    String responseTraceId = tracer.currentSpan() != null ? 
                        tracer.currentSpan().context().traceId() : "no-trace";
                    logger.info("Sending authz response with trace [{}] for path={} method={} allowed={}", 
                        responseTraceId, req.path(), req.method(), response.isAllowed());
                })
                .map(ResponseEntity::ok);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Authorization service is healthy");
    }
}
