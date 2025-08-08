package com.fintech.authservice.dto;

import java.time.LocalDateTime;

public class LoginResponse {
    private boolean success;
    private String userId;
    private String email;
    private String accessToken;
    private LocalDateTime expiresAt;
    private String message;
    private String code;
    private boolean passwordChangeRequired;

    // Private constructor to force use of static factory methods
    private LoginResponse() {}

    // Static factory methods
    public static LoginResponse success(String userId, String email, String accessToken, LocalDateTime expiresAt) {
        LoginResponse response = new LoginResponse();
        response.success = true;
        response.userId = userId;
        response.email = email;
        response.accessToken = accessToken;
        response.expiresAt = expiresAt;
        response.message = "Login successful";
        return response;
    }

    public static LoginResponse passwordChangeRequired(String userId, String message) {
        LoginResponse response = new LoginResponse();
        response.success = false;
        response.passwordChangeRequired = true;
        response.userId = userId;
        response.message = message;
        response.code = "PASSWORD_CHANGE_REQUIRED";
        return response;
    }

    public static LoginResponse failed(String message, String code) {
        LoginResponse response = new LoginResponse();
        response.success = false;
        response.message = message;
        response.code = code;
        return response;
    }

    // Getters
    public boolean isSuccess() { return success; }
    public String getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getAccessToken() { return accessToken; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public String getMessage() { return message; }
    public String getCode() { return code; }
    public boolean isPasswordChangeRequired() { return passwordChangeRequired; }
}
