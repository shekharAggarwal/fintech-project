package com.fintech.userservice.security.util;

import com.fintech.userservice.security.model.AuthorizationContext;

/**
 * Holder for authorization context within the current thread
 */
public class AuthorizationContextHolder {
    
    private static final ThreadLocal<AuthorizationContext> contextHolder = new ThreadLocal<>();
    
    /**
     * Set authorization context for current thread
     */
    public static void setContext(AuthorizationContext context) {
        contextHolder.set(context);
    }
    
    /**
     * Get authorization context for current thread
     */
    public static AuthorizationContext getContext() {
        return contextHolder.get();
    }
    
    /**
     * Clear authorization context for current thread
     */
    public static void clearContext() {
        contextHolder.remove();
    }
    
    /**
     * Check if there's an authorization context
     */
    public static boolean hasContext() {
        return contextHolder.get() != null;
    }
    
    /**
     * Get current user ID from context
     */
    public static String getCurrentUserId() {
        AuthorizationContext context = getContext();
        return context != null ? context.getUserId() : null;
    }
    
    /**
     * Get current user role from context
     */
    public static String getCurrentRole() {
        AuthorizationContext context = getContext();
        return context != null ? context.getRole() : null;
    }
    
    /**
     * Check if current user is admin
     */
    public static boolean isCurrentUserAdmin() {
        AuthorizationContext context = getContext();
        return context != null && context.isAdmin();
    }
    
    /**
     * Check if current user is owner of a resource
     */
    public static boolean isCurrentUserOwner(String resourceUserId) {
        AuthorizationContext context = getContext();
        return context != null && context.isOwner(resourceUserId);
    }
}
