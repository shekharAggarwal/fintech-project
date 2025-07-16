package com.fintech.authorizationservice.dto;

public class AuthorizationResponse {
    private boolean authorized;
    private String reason;
    private String userRole;
    private String userEmail;

    public AuthorizationResponse() {}

    public AuthorizationResponse(boolean authorized, String reason) {
        this.authorized = authorized;
        this.reason = reason;
    }

    public AuthorizationResponse(boolean authorized, String reason, String userRole, String userEmail) {
        this.authorized = authorized;
        this.reason = reason;
        this.userRole = userRole;
        this.userEmail = userEmail;
    }

    public boolean isAuthorized() {
        return authorized;
    }

    public void setAuthorized(boolean authorized) {
        this.authorized = authorized;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
}
