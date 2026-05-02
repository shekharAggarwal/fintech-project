package com.fintech.authservice.controller;

import com.fintech.authservice.dto.request.LoginRequest;
import com.fintech.authservice.dto.request.LogoutRequest;
import com.fintech.authservice.dto.request.RefreshTokenRequest;
import com.fintech.authservice.dto.request.RegistrationRequest;
import com.fintech.authservice.dto.response.*;
import com.fintech.authservice.service.AuthService;
import com.fintech.authservice.service.RefreshTokenService;
import com.fintech.authservice.service.SessionService;
import com.fintech.authservice.util.JwtUtil;
import com.fintech.authservice.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {


    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final SessionService sessionService;

    public AuthController(AuthService authService, JwtUtil jwtUtil, RefreshTokenService refreshTokenService,
                          SessionService sessionService) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
        this.sessionService = sessionService;
    }


    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {

        String ipAddress = SecurityUtils.getClientIpAddress(httpRequest);
        String userAgent = SecurityUtils.getUserAgent(httpRequest);

        AuthenticationResult result = authService.authenticate(request.email(), request.password(), ipAddress, userAgent);

        if (result.success()) {
            String accessToken = jwtUtil.generateAccessToken(result.authCore().getEmail(), result.sessionId());


            // Return response with refresh token
            return ResponseEntity.ok(LoginResponse.success(
                    result.authCore().getUserId(),
                    result.authCore().getEmail(),
                    accessToken,
                    result.refreshToken()));
        } else {
            return ResponseEntity.ok(LoginResponse.failed(result.message(), result.code()));
        }
    }


    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegistrationRequest request) {
        RegistrationResult result = authService.registerUser(request);
        if (result.isSuccess()) {
            return ResponseEntity.ok(RegistrationResponse.success(result.getAuthCore().getUserId(), result.getMessage()));
        } else {
            return ResponseEntity.ok(RegistrationResponse.failed(result.getMessage(), result.getCode()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        RefreshTokenResponse response = refreshTokenService.refreshAccessToken(request.refreshToken());
        if (response.success()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).body(response);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@Valid @RequestBody LogoutRequest request, HttpServletRequest httpRequest) {
        String accessToken = extractBearerToken(httpRequest);
        refreshTokenService.logout(request.refreshToken(), accessToken);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Logged out successfully"
        ));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, Object>> logoutAll(HttpServletRequest httpRequest) {
        String accessToken = extractBearerToken(httpRequest);
        if (accessToken == null || !jwtUtil.validateToken(accessToken)) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Valid authentication required"
            ));
        }

        String sessionId = jwtUtil.getSessionIdFromToken(accessToken);

        String userId = sessionService.getUserIdFromSession(sessionId);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "message", "Session not found"
            ));
        }

        refreshTokenService.logoutAllDevices(userId);

        // Also blacklist the current access token
        refreshTokenService.logout(null, accessToken);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Logged out from all devices successfully"
        ));
    }

    private String extractBearerToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
