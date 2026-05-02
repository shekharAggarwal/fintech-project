package com.fintech.authservice.dto.response;

public record LoginResponse(
        boolean success,
        String userId,
        String email,
        String accessToken,
        String refreshToken,
        String message,
        String code) {

    // Static factory methods
    public static LoginResponse success(String userId, String email, String accessToken, String refreshToken) {
        return new LoginResponse(
                true,
                userId,
                email,
                accessToken,
                refreshToken,
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
                null,
                message,
                code
        );
    }

}
