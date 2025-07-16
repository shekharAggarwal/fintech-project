package com.fintech.authorizationservice.dto;

public class AuthorizationRequest {
    private String token;
    private String resource;
    private String action;

    public AuthorizationRequest() {}

    public AuthorizationRequest(String token, String resource, String action) {
        this.token = token;
        this.resource = resource;
        this.action = action;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
