package com.fintech.authservice.dto.response;

public record RefreshTokenResponse(
        boolean success,
        String accessToken,
        String refreshToken,
        String message,
        String code) {

    public static RefreshTokenResponse success(String accessToken, String refreshToken) {
        return new RefreshTokenResponse(true, accessToken, refreshToken, "Token refreshed successfully", "SUCCESS");
    }

    public static RefreshTokenResponse failed(String message, String code) {
        return new RefreshTokenResponse(false, null, null, message, code);
    }
}
