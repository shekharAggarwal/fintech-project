package com.fintech.authorizationservice.model;

import java.util.List;
import java.util.Map;

public class RoleAuthzCacheData {
    private String role;
    private List<String> permissions;
    private Map<String, Map<String, Object>> resourceAccess;
    private String reason; // For denied responses

    public RoleAuthzCacheData() {
    }

    public RoleAuthzCacheData(String role, List<String> permissions, Map<String, Map<String, Object>> resourceAccess, String reason) {
        this.role = role;
        this.permissions = permissions;
        this.resourceAccess = resourceAccess;
        this.reason = reason;
    }

    // Getters and setters
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public Map<String, Map<String, Object>> getResourceAccess() {
        return resourceAccess;
    }

    public void setResourceAccess(Map<String, Map<String, Object>> resourceAccess) {
        this.resourceAccess = resourceAccess;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean isAllowed() {
        return reason == null;
    }
}