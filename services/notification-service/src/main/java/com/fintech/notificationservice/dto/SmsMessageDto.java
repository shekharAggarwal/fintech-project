package com.fintech.notificationservice.dto;

import java.io.Serializable;

public class SmsMessageDto implements Serializable {

    private String phoneNumber;
    private String message;
    private String type;
    private String userId;

    public SmsMessageDto() {}

    public SmsMessageDto(String phoneNumber, String message, String type, String userId) {
        this.phoneNumber = phoneNumber;
        this.message = message;
        this.type = type;
        this.userId = userId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
