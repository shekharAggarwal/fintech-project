package com.fintech.authorizationservice.dto.response.admin;

public class FieldAccessResponse {

    private Long id;
    private Long roleId;
    private String resourceType;
    private String allowedFields;
    private String config;

    public FieldAccessResponse() {
    }

    public FieldAccessResponse(Long id, Long roleId, String resourceType, String allowedFields, String config) {
        this.id = id;
        this.roleId = roleId;
        this.resourceType = resourceType;
        this.allowedFields = allowedFields;
        this.config = config;
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

    public static FieldAccessResponse from(com.fintech.authorizationservice.entity.FieldAccess fa) {
        return new FieldAccessResponse(fa.getId(), fa.getRole(), fa.getResourceType(), fa.getAllowedFields(), fa.getConfig());
    }
}
