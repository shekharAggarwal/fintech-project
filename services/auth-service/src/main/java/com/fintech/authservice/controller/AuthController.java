package com.fintech.authservice.controller;

import com.fintech.authservice.dto.request.LoginRequest;
import com.fintech.authservice.dto.request.RegistrationRequest;
import com.fintech.authservice.dto.response.AuthenticationResult;
import com.fintech.authservice.dto.response.LoginResponse;
import com.fintech.authservice.dto.response.RegistrationResponse;
import com.fintech.authservice.dto.response.RegistrationResult;
import com.fintech.authservice.service.AuthService;
import com.fintech.authservice.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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


    private final AuthService authService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
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

        if (result.success()) {
            String accessToken = jwtUtil.generateAccessToken(
                    result.sessionId()
            );


            // Return response
            return ResponseEntity.ok(LoginResponse.success(
                    result.authCore().getUserId(),
                    result.authCore().getEmail(),
                    accessToken
            ));
        } else {
            return ResponseEntity.ok(LoginResponse.failed(result.message(), result.code()));
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
}
