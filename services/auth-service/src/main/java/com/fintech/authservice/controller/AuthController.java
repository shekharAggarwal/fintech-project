package com.fintech.authservice.controller;

import com.fintech.authservice.dto.*;
import com.fintech.authservice.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent") != null ? request.getHeader("User-Agent") : "Unknown";
    }

    private String getDeviceFingerprint(HttpServletRequest request) {
        return request.getHeader("X-Device-Fingerprint") != null ? 
               request.getHeader("X-Device-Fingerprint") : "Unknown";
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegistrationRequest request, 
                                                HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = getUserAgent(httpRequest);
        
        AuthResponse response = authService.register(request, ipAddress, userAgent);

        if (response.getMessage() != null && response.getMessage().contains("already exists")) {
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request, 
                                            HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = getUserAgent(httpRequest);
        String deviceFingerprint = getDeviceFingerprint(httpRequest);
        
        AuthResponse response = authService.login(request, ipAddress, userAgent, deviceFingerprint);

        if (response.getAccessToken() == null) {
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody TokenRefreshRequest request,
                                                    HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = getUserAgent(httpRequest);
        
        AuthResponse response = authService.refreshToken(request, ipAddress, userAgent);

        if (response.getAccessToken() == null) {
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(HttpServletRequest httpRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        String sessionId = httpRequest.getSession().getId();
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = getUserAgent(httpRequest);

        AuthResponse response = authService.logout(email, sessionId, ipAddress, userAgent);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    public ResponseEntity<AuthResponse> changePassword(@RequestBody ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        AuthResponse response = authService.changePassword(email, request);

        if (response.getMessage().contains("incorrect")) {
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete-account")
    public ResponseEntity<AuthResponse> deleteAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        AuthResponse response = authService.deleteAccount(email);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate-session/{sessionId}")
    public ResponseEntity<Boolean> validateSession(@PathVariable String sessionId) {
        boolean isValid = authService.validateSession(sessionId);
        return ResponseEntity.ok(isValid);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Auth Service is running");
    }
}
