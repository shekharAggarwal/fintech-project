package com.fintech.authservice.dto;

import jakarta.validation.constraints.NotBlank;

public class LogoutAllRequest {
    @NotBlank(message = "User ID is required")
    private String userId;

    public LogoutAllRequest() {}
    
    public LogoutAllRequest(String userId) {
        this.userId = userId;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
