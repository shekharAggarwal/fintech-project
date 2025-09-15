package com.fintech.userservice.external.model.request;

public record UpdateRoleRequest(String userId, String newRole, String updatedBy, String serviceSource) {
}
