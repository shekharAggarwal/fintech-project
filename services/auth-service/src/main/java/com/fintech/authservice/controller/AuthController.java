package com.fintech.authservice.controller;

import com.fintech.authservice.dto.*;
import com.fintech.authservice.service.AuthService;
import com.fintech.authservice.service.AuthService.AuthenticationResult;
import com.fintech.authservice.service.AuthService.RegistrationResult;
import com.fintech.authservice.service.AuthService.SessionValidationResult;
import com.fintech.authservice.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.fintech.authservice.util.SecurityUtils.getClientIpAddress;
import static com.fintech.authservice.util.SecurityUtils.getUserAgent;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    final private AuthService authService;

    @Autowired
    private JwtUtil jwtUtil;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }


    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = getUserAgent(httpRequest);

        AuthenticationResult result = authService.authenticate(
                request.getEmail(),
                request.getPassword(),
                ipAddress,
                userAgent
        );

        if (result.isSuccess()) {
            String jwt = jwtUtil.generateAccessToken(result.getAuthCore().getEmail(), result.getSession().getSessionId());
            return ResponseEntity.ok(LoginResponse.success(
                    result.getAuthCore().getUserId(),
                    result.getAuthCore().getEmail(),
                    jwt,
                    result.getSession().getExpiresAt()
            ));
        } else if (result.isPasswordChangeRequired()) {
            return ResponseEntity.ok(LoginResponse.passwordChangeRequired(
                    result.getAuthCore().getUserId(),
                    result.getMessage()
            ));
        } else {
            return ResponseEntity.ok(LoginResponse.failed(result.getMessage(), result.getCode()));
        }
    }


    @PostMapping("/validate")
    public ResponseEntity<SessionValidationResponse> validateSession(@Valid @RequestBody SessionValidationRequest request) {
        SessionValidationResult result = authService.validateSession(request.getSessionId());

        if (result.isValid()) {
            return ResponseEntity.ok(SessionValidationResponse.valid(
                    result.getAuthCore().getUserId(),
                    result.getAuthCore().getEmail(),
                    result.getAuthCore().getStatus().toString(),
                    result.getSession().getExpiresAt()
            ));
        } else {
            return ResponseEntity.ok(SessionValidationResponse.invalid(result.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegistrationRequest request) {
        RegistrationResult result = authService.registerUser(request);
        if (result.isSuccess()) {
            return ResponseEntity.ok(RegistrationResponse.success(
                    result.getAuthCore().getUserId(),
                    result.getMessage()
            ));
        } else {
            return ResponseEntity.ok(RegistrationResponse.failed(result.getMessage(), result.getCode()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.getSessionId());
        return ResponseEntity.ok(LogoutResponse.success("Logged out successfully"));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<LogoutResponse> logoutAll(@Valid @RequestBody LogoutAllRequest request) {
        authService.logoutAllDevices(request.getUserId());
        return ResponseEntity.ok(LogoutResponse.success("Logged out from all devices"));
    }
}
