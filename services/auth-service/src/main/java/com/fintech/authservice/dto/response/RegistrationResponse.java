package com.fintech.authservice.dto.response;

public class RegistrationResponse {
    private boolean success;
    private String userId;
    private String message;
    private String code;

    private RegistrationResponse() {}

    public static RegistrationResponse success(String userId, String message) {
        RegistrationResponse response = new RegistrationResponse();
        response.success = true;
        response.userId = userId;
        response.message = message;
        return response;
    }

    public static RegistrationResponse failed(String message, String code) {
        RegistrationResponse response = new RegistrationResponse();
        response.success = false;
        response.message = message;
        response.code = code;
        return response;
    }

    // Getters
    public boolean isSuccess() { return success; }
    public String getUserId() { return userId; }
    public String getMessage() { return message; }
    public String getCode() { return code; }
}
