package com.fintech.authservice.dto;

import java.time.LocalDateTime;

public class SessionValidationResponse {
    private boolean valid;
    private String userId;
    private String email;
    private String status;
    private LocalDateTime expiresAt;
    private String message;

    private SessionValidationResponse() {}

    public static SessionValidationResponse valid(String userId, String email, String status, LocalDateTime expiresAt) {
        SessionValidationResponse response = new SessionValidationResponse();
        response.valid = true;
        response.userId = userId;
        response.email = email;
        response.status = status;
        response.expiresAt = expiresAt;
        response.message = "Session is valid";
        return response;
    }

    public static SessionValidationResponse invalid(String message) {
        SessionValidationResponse response = new SessionValidationResponse();
        response.valid = false;
        response.message = message;
        return response;
    }

    // Getters
    public boolean isValid() { return valid; }
    public String getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getStatus() { return status; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public String getMessage() { return message; }
}
