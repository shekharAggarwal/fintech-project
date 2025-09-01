package com.fintech.authorizationservice.dto.response;

import java.util.List;
import java.util.Map;

public class AuthzIntrospectResponse {
    private boolean allowed;
    private String userId;
    private String role;
    private List<String> permissions;
    private Map<String, Object> limits; // perTxnMax, dailyMax etc
    private Map<String, List<String>> fieldAccess; // resource -> fields
    private String reason;

    public AuthzIntrospectResponse() {
    }

    public AuthzIntrospectResponse(boolean allowed, String userId, String role, List<String> permissions, Map<String, Object> limits, Map<String, List<String>> fieldAccess, String reason) {
        this.allowed = allowed;
        this.userId = userId;
        this.role = role;
        this.permissions = permissions;
        this.limits = limits;
        this.fieldAccess = fieldAccess;
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

    public Map<String, Object> getLimits() {
        return limits;
    }

    public void setLimits(Map<String, Object> limits) {
        this.limits = limits;
    }

    public Map<String, List<String>> getFieldAccess() {
        return fieldAccess;
    }

    public void setFieldAccess(Map<String, List<String>> fieldAccess) {
        this.fieldAccess = fieldAccess;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
