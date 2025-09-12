package com.fintech.userservice.dto.message;

public class UserGreetingNotification {
    private String email;
    private String firstName;
    private String lastName;
    private String userId;
    private String accountNumber;
    private long timestamp;
    private String notificationType;

    public UserGreetingNotification() {
        this.notificationType = "USER_GREETING";
    }

    public UserGreetingNotification(String email, String firstName, String lastName, String userId, String accountNumber, long timestamp) {
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.userId = userId;
        this.accountNumber = accountNumber;
        this.timestamp = timestamp;
        this.notificationType = "USER_GREETING";
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
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
}
