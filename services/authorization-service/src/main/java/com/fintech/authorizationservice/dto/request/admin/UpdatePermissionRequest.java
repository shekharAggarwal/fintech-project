package com.fintech.authorizationservice.dto.request.admin;

public class UpdatePermissionRequest {

    private Boolean allowed;

    public UpdatePermissionRequest() {
    }

    public UpdatePermissionRequest(Boolean allowed) {
        this.allowed = allowed;
    }

    public Boolean getAllowed() {
        return allowed;
    }

    public void setAllowed(Boolean allowed) {
        this.allowed = allowed;
    }
}
