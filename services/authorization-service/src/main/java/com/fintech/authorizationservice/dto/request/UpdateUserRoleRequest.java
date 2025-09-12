package com.fintech.authorizationservice.dto.request;

/**
 * DTO for updating user role via internal service calls
 * This API should only be accessible from internal services in a closed environment
 */
public class UpdateUserRoleRequest {
    private String userId;
    private String newRole;
    private String updatedBy;
    private String serviceSource; // To identify which service is making the call

    public UpdateUserRoleRequest() {
    }

    public UpdateUserRoleRequest(String userId, String newRole, String updatedBy, String serviceSource) {
        this.userId = userId;
        this.newRole = newRole;
        this.updatedBy = updatedBy;
        this.serviceSource = serviceSource;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getNewRole() {
        return newRole;
    }

    public void setNewRole(String newRole) {
        this.newRole = newRole;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getServiceSource() {
        return serviceSource;
    }

    public void setServiceSource(String serviceSource) {
        this.serviceSource = serviceSource;
    }
}
