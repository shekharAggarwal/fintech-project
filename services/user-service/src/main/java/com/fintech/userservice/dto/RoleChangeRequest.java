package com.fintech.userservice.dto;

/**
 * Request object for changing user roles
 */
public class RoleChangeRequest {
    
    private String oldRole;
    private String newRole;
    private String reason; // Optional reason for the change
    
    // Default constructor
    public RoleChangeRequest() {}
    
    public RoleChangeRequest(String oldRole, String newRole) {
        this.oldRole = oldRole;
        this.newRole = newRole;
    }
    
    public RoleChangeRequest(String oldRole, String newRole, String reason) {
        this.oldRole = oldRole;
        this.newRole = newRole;
        this.reason = reason;
    }
    
    // Getters and Setters
    public String getOldRole() { return oldRole; }
    public void setOldRole(String oldRole) { this.oldRole = oldRole; }
    
    public String getNewRole() { return newRole; }
    public void setNewRole(String newRole) { this.newRole = newRole; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    @Override
    public String toString() {
        return String.format("RoleChangeRequest{oldRole='%s', newRole='%s', reason='%s'}", 
                           oldRole, newRole, reason);
    }
}
