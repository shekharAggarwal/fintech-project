package com.fintech.authorizationservice.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class SessionCacheData {
    private String userId;
    private Long roleId;
    private String roleName;
    private long validationTime;

    public SessionCacheData() {
    }

    public SessionCacheData(String userId, Long roleId, String roleName, long validationTime) {
        this.userId = userId;
        this.roleId = roleId;
        this.roleName = roleName;
        this.validationTime = validationTime;
    }

    // Getters and setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public long getValidationTime() {
        return validationTime;
    }

    public void setValidationTime(long validationTime) {
        this.validationTime = validationTime;
    }
    @JsonIgnore
    public boolean isValid() {
        return System.currentTimeMillis() < this.validationTime;
    }
}