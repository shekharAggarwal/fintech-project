package com.fintech.userservice.dto;

/**
 * Response from authorization service
 */
public class AuthorizationResponse {
    
    private boolean authorized;
    private String reason;
    private String userRole;
    private String userEmail;
    
    // Default constructor
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
    
    // Getters and Setters
    public boolean isAuthorized() { return authorized; }
    public void setAuthorized(boolean authorized) { this.authorized = authorized; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }
    
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    
    @Override
    public String toString() {
        return String.format("AuthorizationResponse{authorized=%s, reason='%s', userRole='%s', userEmail='%s'}", 
                           authorized, reason, userRole, userEmail);
    }
}
