package com.fintech.security.service;

import com.fintech.security.model.AuthorizationContext;
import com.fintech.security.util.AuthorizationContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enhanced Authorization Service that works with database-driven authorization data from gateway
 * Understands the dynamic nature of roles, permissions, and field access from the database
 */
@Service
public class AuthorizationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationService.class);

    /**
     * Enhanced authorization check with database-driven permissions
     * Works with the authorization context from gateway
     */
    public boolean hasPermission(String userId, String path, String method) {
        AuthorizationContext context = AuthorizationContextHolder.getContext();
        if (context == null || !context.isAllowed()) {
            return false;
        }

        // Check if the user ID matches (ownership check)
        if (!userId.equals(context.getUserId())) {
            // Check if user has full access level for user resource
            return context.hasFullAccess("user");
        }

        return true;
    }

    /**
     * Check if user can access a specific field based on database-driven field access config
     */
    public boolean hasFieldAccess(String resourceType, String fieldName) {
        AuthorizationContext context = AuthorizationContextHolder.getContext();
        if (context == null || !context.isAllowed()) {
            return false;
        }

        // Use the database-driven field access configuration
        return context.hasFieldAccess(resourceType, fieldName);
    }


    /**
     * Check access level (self_only, full, etc.) from database config
     */
    public boolean hasAccessLevel(String accessLevel, String resourceType) {
        AuthorizationContext context = AuthorizationContextHolder.getContext();
        if (context == null || !context.isAllowed()) {
            return false;
        }

        // Check access level from database configuration for specific resource type
        if (context.getResourceAccess() != null) {
            Map<String, Map<String, Object>> resourceAccess = context.getResourceAccess();
            if (resourceAccess.containsKey(resourceType)) {
                Map<String, Object> resourceConfig = resourceAccess.get(resourceType);
                if (resourceConfig != null && resourceConfig.containsKey("config")) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> config = (Map<String, Object>) resourceConfig.get("config");
                    if (config != null && config.containsKey("access_level")) {
                        String dbAccessLevel = (String) config.get("access_level");
                        return accessLevel.equalsIgnoreCase(dbAccessLevel);
                    }
                }
            }
        }

        return false;
    }


    /**
     * Check if user can modify specific fields based on database config
     */
    public boolean canModifyField(String resourceType, String fieldName) {
        AuthorizationContext context = AuthorizationContextHolder.getContext();
        if (context == null || !context.isAllowed()) {
            return false;
        }

        // Use the new canModifyField method from AuthorizationContext
        return context.canModifyField(resourceType, fieldName);
    }

    /**
     * Filter object fields based on database-driven field access
     */
    public Map<String, Object> filterFields(Map<String, Object> data, String resourceType) {
        AuthorizationContext context = AuthorizationContextHolder.getContext();
        if (context == null || !context.isAllowed()) {
            return Map.of();
        }

        List<String> allowedFields = context.getAllowedFields(resourceType);
        Map<String, Object> filtered = new HashMap<>();

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String fieldName = entry.getKey();

            // Only include fields that are explicitly allowed in database configuration
            if (allowedFields.contains("*") || allowedFields.contains(fieldName)) {
                filtered.put(fieldName, entry.getValue());
            }
            // Field access is controlled by the database configuration
            // If a field is not in allowedFields, it won't be included
        }

        return filtered;
    }

    /**
     * Validate if current user can access target user's data
     */
    public boolean canAccessUserData(String targetUserId) {
        AuthorizationContext context = AuthorizationContextHolder.getContext();
        if (context == null || !context.isAllowed()) {
            return false;
        }

        // Check if user has full access to user resource or if it's their own data
        return context.hasFullAccess("user") || context.getUserId().equals(targetUserId);
    }

    /**
     * Check if current user can modify target user's data
     */
    public boolean canModifyUserData(String targetUserId) {
        AuthorizationContext context = AuthorizationContextHolder.getContext();
        if (context == null || !context.isAllowed()) {
            return false;
        }

        // Check if user has full access to user resource or if it's their own data
        return context.hasFullAccess("user") || context.getUserId().equals(targetUserId);
    }

    /**
     * Get current user's permissions from context
     */
    public List<String> getCurrentPermissions() {
        AuthorizationContext context = AuthorizationContextHolder.getContext();
        return context != null ? context.getPermissions() : List.of();
    }

    /**
     * Check if user has specific permission
     */
    public boolean hasSpecificPermission(String permission) {
        return getCurrentPermissions().contains(permission);
    }

    /**
     * Get user's limits (from database: perTxnMax, dailyMax, etc.)
     *
     * @deprecated Use getResourceConfig instead
     */
    @Deprecated
    public Object getLimit(String limitType) {
        AuthorizationContext context = AuthorizationContextHolder.getContext();
        if (context == null) {
            return null;
        }
        return context.getLimit(limitType);
    }

    /**
     * Get resource configuration value
     */
    public Object getResourceConfig(String resourceType, String configKey) {
        AuthorizationContext context = AuthorizationContextHolder.getContext();
        if (context == null) {
            return null;
        }
        return context.getResourceConfig(resourceType, configKey);
    }

    /**
     * Validate against limits
     */
    public boolean validateLimit(String limitType, Object value) {
        // Try to find the limit in any resource config
        Object limit = getLimit(limitType);
        if (limit == null) {
            return true; // No limit defined
        }

        try {
            if (limit instanceof Number && value instanceof Number) {
                return ((Number) value).doubleValue() <= ((Number) limit).doubleValue();
            }
            return true;
        } catch (Exception e) {
            logger.warn("Error validating limit {} with value {}", limitType, value, e);
            return false;
        }
    }

    /**
     * Validate against resource-specific limits
     */
    public boolean validateResourceLimit(String resourceType, String limitType, Object value) {
        Object limit = getResourceConfig(resourceType, limitType);
        if (limit == null) {
            return true; // No limit defined
        }

        try {
            if (limit instanceof Number && value instanceof Number) {
                return ((Number) value).doubleValue() <= ((Number) limit).doubleValue();
            }
            return true;
        } catch (Exception e) {
            logger.warn("Error validating resource limit {} for resource {} with value {}", limitType, resourceType, value, e);
            return false;
        }
    }

    /**
     * Check if user has specific role (supports database role names)
     */
    public boolean hasRole(String roleName) {
        AuthorizationContext context = AuthorizationContextHolder.getContext();
        if (context == null || !context.isAllowed()) {
            return false;
        }

        String userRole = context.getRole();
        if (userRole == null) {
            return false;
        }

        // Support both uppercase and lowercase role names from database
        return userRole.equalsIgnoreCase(roleName) ||
                userRole.equalsIgnoreCase("ROLE_" + roleName) ||
                userRole.equalsIgnoreCase(roleName.toUpperCase());
    }

    /**
     * Get current user ID
     */
    public String getCurrentUserId() {
        AuthorizationContext context = AuthorizationContextHolder.getContext();
        return context != null ? context.getUserId() : null;
    }

    /**
     * Get current user role
     */
    public String getCurrentRole() {
        AuthorizationContext context = AuthorizationContextHolder.getContext();
        return context != null ? context.getRole() : null;
    }

    /**
     * Check if user is authenticated and authorized
     */
    public boolean isAuthorized() {
        AuthorizationContext context = AuthorizationContextHolder.getContext();
        return context != null && context.isAllowed();
    }
}