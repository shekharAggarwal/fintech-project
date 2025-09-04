package com.fintech.authorizationservice.dto.response;

import java.util.List;
import java.util.Map;

public class AuthzIntrospectResponse {
    private boolean allowed;
    private String userId;
    private String role;
    private List<String> permissions;
    private Map<String, Map<String, Object>> resourceAccess; // resourceType -> {access_field: [], config: {}}
    private String reason;

    public AuthzIntrospectResponse() {
    }

    public AuthzIntrospectResponse(boolean allowed, String userId, String role, List<String> permissions, Map<String, Map<String, Object>> resourceAccess, String reason) {
        this.allowed = allowed;
        this.userId = userId;
        this.role = role;
        this.permissions = permissions;
        this.resourceAccess = resourceAccess;
        this.reason = reason;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

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
}
