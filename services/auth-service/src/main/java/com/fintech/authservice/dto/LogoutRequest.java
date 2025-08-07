package com.fintech.authservice.dto;

import jakarta.validation.constraints.NotBlank;

public class LogoutRequest {
    @NotBlank(message = "Session ID is required")
    private String sessionId;

    public LogoutRequest() {}
    
    public LogoutRequest(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
}
