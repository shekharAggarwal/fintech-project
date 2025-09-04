package com.fintech.userservice.security.model;

import java.util.List;
import java.util.Map;

/**
 * Authorization context that holds the authorization information from gateway
 */


public class AuthorizationContext {
    private boolean allowed;
    private String userId;
    private String role;
    private List<String> permissions;
    private Map<String, Map<String, Object>> resourceAccess; // resourceType -> {access_field: [], config: {}}
    private String reason;

    public AuthorizationContext() {}

    public AuthorizationContext(boolean allowed, String userId, String role, 
                              List<String> permissions, Map<String, Map<String, Object>> resourceAccess, String reason) {
        this.allowed = allowed;
        this.userId = userId;
        this.role = role;
        this.permissions = permissions;
        this.resourceAccess = resourceAccess;
        this.reason = reason;
    }

    // Getters and setters
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

    public Map<String, Map<String, Object>> getResourceAccess() {
        return resourceAccess;
    }

    public void setResourceAccess(Map<String, Map<String, Object>> resourceAccess) {
        this.resourceAccess = resourceAccess;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    /**
     * Check if user has a specific role
     */
    public boolean hasRole(String role) {
        return this.role != null && this.role.equalsIgnoreCase(role);
    }

    /**
     * Check if user has admin privileges
     */
    public boolean isAdmin() {
        return hasRole("admin");
    }

    /**
     * Check if user can access a specific field for a resource type
     */
    public boolean hasFieldAccess(String resourceType, String field) {
        if (resourceAccess == null) {
            return false;
        }
        
        Map<String, Object> resource = resourceAccess.get(resourceType.toLowerCase());
        if (resource == null) {
            return false;
        }
        
        @SuppressWarnings("unchecked")
        List<String> allowedFields = (List<String>) resource.get("access_field");
        if (allowedFields == null) {
            return false;
        }
        
        // Check for wildcard access
        if (allowedFields.contains("*")) {
            return true;
        }
        
        return allowedFields.contains(field);
    }

    /**
     * Get allowed fields for a resource type
     */
    public List<String> getAllowedFields(String resourceType) {
        if (resourceAccess == null) {
            return List.of();
        }
        
        Map<String, Object> resource = resourceAccess.get(resourceType.toLowerCase());
        if (resource == null) {
            return List.of();
        }
        
        @SuppressWarnings("unchecked")
        List<String> allowedFields = (List<String>) resource.get("access_field");
        return allowedFields != null ? allowedFields : List.of();
    }

    /**
     * Check if the current user is accessing their own resource
     */
    public boolean isOwner(String resourceUserId) {
        return this.userId != null && this.userId.equals(resourceUserId);
    }

    /**
     * Get limit value for a specific limit type
     * @deprecated Use getResourceConfig instead
     */
    @Deprecated
    public Object getLimit(String limitType) {
        // For backward compatibility, check all resource configs
        if (resourceAccess == null) {
            return null;
        }
        
        for (Map<String, Object> resource : resourceAccess.values()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> config = (Map<String, Object>) resource.get("config");
            if (config != null && config.containsKey(limitType)) {
                return config.get(limitType);
            }
        }
        return null;
    }

    /**
     * Get config value for a specific resource type and config key
     */
    public Object getResourceConfig(String resourceType, String configKey) {
        if (resourceAccess == null) {
            return null;
        }
        
        Map<String, Object> resource = resourceAccess.get(resourceType.toLowerCase());
        if (resource == null) {
            return null;
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> config = (Map<String, Object>) resource.get("config");
        return config != null ? config.get(configKey) : null;
    }

    /**
     * Get all config for a specific resource type
     */
    public Map<String, Object> getResourceConfig(String resourceType) {
        if (resourceAccess == null) {
            return Map.of();
        }
        
        Map<String, Object> resource = resourceAccess.get(resourceType.toLowerCase());
        if (resource == null) {
            return Map.of();
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> config = (Map<String, Object>) resource.get("config");
        return config != null ? config : Map.of();
    }

    /**
     * Check if user can modify a specific field for a resource type
     */
    public boolean canModifyField(String resourceType, String field) {
        Object canModify = getResourceConfig(resourceType, "can_modify");
        
        if (canModify instanceof Boolean) {
            return (Boolean) canModify; // Admin case: can_modify: true
        }
        
        if (canModify instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> modifiableFields = (List<String>) canModify;
            return modifiableFields.contains(field);
        }
        
        return false;
    }

    /**
     * Check if user has full access to a resource type
     */
    public boolean hasFullAccess(String resourceType) {
        String accessLevel = (String) getResourceConfig(resourceType, "access_level");
        return "full".equals(accessLevel);
    }

    /**
     * Check if user has self-only access to a resource type
     */
    public boolean hasSelfOnlyAccess(String resourceType) {
        String accessLevel = (String) getResourceConfig(resourceType, "access_level");
        return "self_only".equals(accessLevel);
    }

    @Override
    public String toString() {
        return "AuthorizationContext{" +
                "allowed=" + allowed +
                ", userId='" + userId + '\'' +
                ", role='" + role + '\'' +
                ", permissions=" + permissions +
                ", resourceAccess=" + resourceAccess +
                ", reason='" + reason + '\'' +
                '}';
    }
}
