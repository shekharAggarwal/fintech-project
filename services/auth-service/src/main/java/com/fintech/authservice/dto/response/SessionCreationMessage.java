package com.fintech.authservice.dto.response;

import java.time.LocalDateTime;

public class SessionCreationMessage {
    private String sessionId;
    private String userId;
    private Long expiryTime;
    private LocalDateTime createdAt;
    private String ipAddress;
    private String userAgent;

    public SessionCreationMessage() {
    }

    public SessionCreationMessage(String sessionId, String userId, Long expiryTime, String ipAddress, String userAgent) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.expiryTime = expiryTime;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.createdAt = LocalDateTime.now();
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Long getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(Long expiryTime) {
        this.expiryTime = expiryTime;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}
