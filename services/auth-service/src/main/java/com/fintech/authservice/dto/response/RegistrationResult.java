package com.fintech.authservice.dto.response;

import com.fintech.authservice.entity.AuthCore;

public class RegistrationResult {
    private final boolean success;
    private final String message;
    private final String code;
    private final AuthCore authCore;

    private RegistrationResult(boolean success, String message, String code, AuthCore authCore) {
        this.success = success;
        this.message = message;
        this.code = code;
        this.authCore = authCore;
    }

    public static RegistrationResult success(AuthCore authCore, String message) {
        return new RegistrationResult(true, message, "SUCCESS", authCore);
    }

    public static RegistrationResult failed(String message, String code) {
        return new RegistrationResult(false, message, code, null);
    }

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getCode() {
        return code;
    }

    public AuthCore getAuthCore() {
        return authCore;
    }
}