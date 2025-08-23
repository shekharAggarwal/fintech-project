package com.fintech.authservice.dto.response;

public class LogoutResponse {
    private boolean success;
    private String message;

    private LogoutResponse() {}

    public static LogoutResponse success(String message) {
        LogoutResponse response = new LogoutResponse();
        response.success = true;
        response.message = message;
        return response;
    }

    public static LogoutResponse failed(String message) {
        LogoutResponse response = new LogoutResponse();
        response.success = false;
        response.message = message;
        return response;
    }

    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
}
