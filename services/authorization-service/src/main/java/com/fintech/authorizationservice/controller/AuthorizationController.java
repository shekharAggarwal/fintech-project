package com.fintech.authorizationservice.controller;

import com.fintech.authorizationservice.dto.AuthorizationRequest;
import com.fintech.authorizationservice.dto.AuthorizationResponse;
import com.fintech.authorizationservice.service.AuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/authorization")
public class AuthorizationController {

    @Autowired
    private AuthorizationService authorizationService;

    @PostMapping("/authorize")
    public ResponseEntity<AuthorizationResponse> authorize(@RequestBody AuthorizationRequest request) {
        AuthorizationResponse response = authorizationService.authorize(request);
        
        if (response.isAuthorized()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(403).body(response);
        }
    }

    @GetMapping("/check")
    public ResponseEntity<AuthorizationResponse> checkPermission(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam String resource,
            @RequestParam(required = false, defaultValue = "view") String action) {
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401)
                    .body(new AuthorizationResponse(false, "Missing or invalid Authorization header"));
        }

        String token = authHeader.substring(7);
        AuthorizationResponse response = authorizationService.checkPermission(token, resource, action);
        
        if (response.isAuthorized()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(403).body(response);
        }
    }

    @GetMapping("/authorities")
    public ResponseEntity<Set<String>> getUserAuthorities(
            @RequestHeader("Authorization") String authHeader) {
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Set.of());
        }

        String token = authHeader.substring(7);
        Set<String> authorities = authorizationService.getUserAuthorities(token);
        
        return ResponseEntity.ok(authorities);
    }
}
