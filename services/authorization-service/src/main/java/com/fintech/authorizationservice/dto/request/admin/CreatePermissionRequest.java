package com.fintech.authorizationservice.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreatePermissionRequest {

    @NotNull(message = "Role ID is required")
    private Long roleId;

    @NotNull(message = "API method ID is required")
    private Long apiMethodId;

    private boolean allowed = true;

    public CreatePermissionRequest() {
    }

    public CreatePermissionRequest(Long roleId, Long apiMethodId, boolean allowed) {
        this.roleId = roleId;
        this.apiMethodId = apiMethodId;
        this.allowed = allowed;
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
}
