package com.fintech.authorizationservice.dto.response.admin;

public class RoleResponse {

    private Long roleId;
    private String name;
    private String description;

    public RoleResponse() {
    }

    public RoleResponse(Long roleId, String name, String description) {
        this.roleId = roleId;
        this.name = name;
        this.description = description;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public static RoleResponse from(com.fintech.authorizationservice.entity.Role role) {
        return new RoleResponse(role.getRoleId(), role.getName(), role.getDescription());
    }
}
