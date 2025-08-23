package com.fintech.authservice.dto.response;

public record LoginResponse(
        boolean success,
        String userId,
        String email,
        String accessToken,
        String message,
        String code) {

    // Static factory methods
    public static LoginResponse success(String userId, String email, String accessToken) {
        return new LoginResponse(
                true,
                userId,
                email,
                accessToken,
                "Login successful",
                "SUCCESS"
        );
    }


    public static LoginResponse failed(String message, String code) {
        return new LoginResponse(
                false,
                null,
                null,
                null,
                message,
                code
        );
    }

}
