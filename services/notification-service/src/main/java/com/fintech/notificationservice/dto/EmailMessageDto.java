package com.fintech.notificationservice.dto;

public class EmailMessageDto {
    private String to;
    private String subject;
    private String body;
    private String type;
    
    public EmailMessageDto() {}
    
    public EmailMessageDto(String to, String subject, String body, String type) {
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.type = type;
    }
    
    // Getters and Setters
    public String getTo() {
        return to;
    }
    
    public void setTo(String to) {
        this.to = to;
    }
    
    public String getSubject() {
        return subject;
    }
    
    public void setSubject(String subject) {
        this.subject = subject;
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
