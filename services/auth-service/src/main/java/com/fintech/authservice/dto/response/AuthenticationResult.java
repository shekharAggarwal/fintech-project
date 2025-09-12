package com.fintech.authservice.dto.response;

import com.fintech.authservice.entity.AuthCore;

public record AuthenticationResult(boolean success, String message, String code, AuthCore authCore, String sessionId) {

    public static AuthenticationResult success(AuthCore authCore, String sessionId) {
        return new AuthenticationResult(true, "Authentication successful", "SUCCESS", authCore, sessionId);
    }

    public static AuthenticationResult failed(String message, String code) {
        return new AuthenticationResult(false, message, code, null, null);
    }
}
