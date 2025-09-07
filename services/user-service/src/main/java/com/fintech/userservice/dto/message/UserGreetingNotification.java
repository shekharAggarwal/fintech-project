package com.fintech.userservice.dto.message;

public class UserGreetingNotification {
    private String email;
    private String fullName;
    private String userId;
    private String accountNumber;
    private long timestamp;
    private String notificationType;

    public UserGreetingNotification() {
        this.notificationType = "USER_GREETING";
    }

    public UserGreetingNotification(String email, String fullName, String userId, String accountNumber, long timestamp) {
        this.email = email;
        this.fullName = fullName;
        this.userId = userId;
        this.accountNumber = accountNumber;
        this.timestamp = timestamp;
        this.notificationType = "USER_GREETING";
    }

    // Getters and setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
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
        return "UserGreetingNotification{" +
                "email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", userId='" + userId + '\'' +
                ", accountNumber='" + accountNumber + '\'' +
                ", timestamp=" + timestamp +
                ", notificationType='" + notificationType + '\'' +
                '}';
    }
}
