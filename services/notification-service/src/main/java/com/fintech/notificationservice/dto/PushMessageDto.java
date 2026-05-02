package com.fintech.notificationservice.dto;

import java.io.Serializable;

public class PushMessageDto implements Serializable {

    private String userId;
    private String title;
    private String body;
    private String type;

    public PushMessageDto() {}

    public PushMessageDto(String userId, String title, String body, String type) {
        this.userId = userId;
        this.title = title;
        this.body = body;
        this.type = type;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
