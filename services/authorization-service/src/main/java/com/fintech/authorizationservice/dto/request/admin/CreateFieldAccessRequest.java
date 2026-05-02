package com.fintech.authorizationservice.dto.request.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateFieldAccessRequest {

    @NotNull(message = "Role ID is required")
    private Long roleId;

    @NotBlank(message = "Resource type is required")
    private String resourceType;

    private String allowedFields;

    private String config;

    public CreateFieldAccessRequest() {
    }

    public CreateFieldAccessRequest(Long roleId, String resourceType, String allowedFields, String config) {
        this.roleId = roleId;
        this.resourceType = resourceType;
        this.allowedFields = allowedFields;
        this.config = config;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getAllowedFields() {
        return allowedFields;
    }

    public void setAllowedFields(String allowedFields) {
        this.allowedFields = allowedFields;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }
}
