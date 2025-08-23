package com.fintech.authorizationservice.dto.response;

import java.util.List;
import java.util.Map;

public class AuthzIntrospectResponse {
    private boolean allowed;
    private String userId;
    private List<String> roles;
    private List<String> permissions;
    private Map<String, Object> limits; // perTxnMax, dailyMax etc
    private Map<String, List<String>> fieldAccess; // resource -> fields
    private String policyVersion;
    private Integer cacheTtlSeconds;
    private String reason;

    public AuthzIntrospectResponse(boolean allowed, String userId, List<String> roles, List<String> permissions, Map<String, Object> limits, Map<String, List<String>> fieldAccess, String policyVersion, Integer cacheTtlSeconds, String reason) {
        this.allowed = allowed;
        this.userId = userId;
        this.roles = roles;
        this.permissions = permissions;
        this.limits = limits;
        this.fieldAccess = fieldAccess;
        this.policyVersion = policyVersion;
        this.cacheTtlSeconds = cacheTtlSeconds;
        this.reason = reason;
    }

    public AuthzIntrospectResponse() {
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

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
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

    public String getPolicyVersion() {
        return policyVersion;
    }

    public void setPolicyVersion(String policyVersion) {
        this.policyVersion = policyVersion;
    }

    public Integer getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    public void setCacheTtlSeconds(Integer cacheTtlSeconds) {
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
