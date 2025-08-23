package com.fintech.authorizationservice.controller;

import com.fintech.authorizationservice.dto.request.AuthzIntrospectRequest;
import com.fintech.authorizationservice.dto.response.AuthzIntrospectResponse;
import com.fintech.authorizationservice.service.AuthzService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/authz")
public class AuthorizationController {

    private final AuthzService authzService;

    public AuthorizationController(AuthzService authzService) {
        this.authzService = authzService;
    }

    @PostMapping("/introspect")
    public Mono<ResponseEntity<AuthzIntrospectResponse>> introspect(@RequestBody AuthzIntrospectRequest req) {
        return authzService.introspect(req).map(ResponseEntity::ok);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Authorization service is healthy");
    }
}
