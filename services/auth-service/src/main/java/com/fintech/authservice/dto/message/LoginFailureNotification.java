package com.fintech.authservice.dto.message;

public class LoginFailureNotification {
    private String email;
    private String ipAddress;
    private String userAgent;
    private long timestamp;
    private String notificationType;

    public LoginFailureNotification() {
        this.notificationType = "LOGIN_FAILURE";
    }

    public LoginFailureNotification(String email, String ipAddress, String userAgent, long timestamp) {
        this.email = email;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.timestamp = timestamp;
        this.notificationType = "LOGIN_FAILURE";
    }

    // Getters and setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    @Override
    public String toString() {
        return "LoginFailureNotification{" +
                "email='" + email + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", userAgent='" + userAgent + '\'' +
                ", timestamp=" + timestamp +
                ", notificationType='" + notificationType + '\'' +
                '}';
    }
}
