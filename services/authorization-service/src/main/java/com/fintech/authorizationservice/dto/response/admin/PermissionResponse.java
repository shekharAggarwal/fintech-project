package com.fintech.authorizationservice.dto.response.admin;

public class PermissionResponse {

    private Long id;
    private Long roleId;
    private Long apiMethodId;
    private boolean allowed;

    public PermissionResponse() {
    }

    public PermissionResponse(Long id, Long roleId, Long apiMethodId, boolean allowed) {
        this.id = id;
        this.roleId = roleId;
        this.apiMethodId = apiMethodId;
        this.allowed = allowed;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public Long getApiMethodId() {
        return apiMethodId;
    }

    public void setApiMethodId(Long apiMethodId) {
        this.apiMethodId = apiMethodId;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

    public static PermissionResponse from(com.fintech.authorizationservice.entity.RolePermission rp) {
        return new PermissionResponse(rp.getId(), rp.getRole(), rp.getApiMethodId(), rp.isAllowed());
    }
}
