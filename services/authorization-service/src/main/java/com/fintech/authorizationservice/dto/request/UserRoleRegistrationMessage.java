package com.fintech.authorizationservice.dto.request;

public class UserRoleRegistrationMessage {
    private String userId;
    private String role;
    private long timestamp;

    public UserRoleRegistrationMessage() {
    }

    public UserRoleRegistrationMessage(String userId, String role, long timestamp) {
        this.userId = userId;
        this.role = role;
        this.timestamp = timestamp;
    }

    // Getters and setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "UserRoleRegistrationMessage{" +
                "userId='" + userId + '\'' +
                ", role='" + role + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
