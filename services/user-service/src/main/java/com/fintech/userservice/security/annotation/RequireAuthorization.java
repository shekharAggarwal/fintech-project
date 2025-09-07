package com.fintech.userservice.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation for method-level authorization
 * Based on patterns from Spring Security authorization examples
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequireAuthorization {
    
    /**
     * Required roles (OR logic - user needs at least one of these roles)
     */
    String[] roles() default {};
    
    /**
     * Resource type for field-level access control
     */
    String resourceType() default "";

    /**
     * Custom authorization expression (SpEL-like)
     */
    String expression() default "";
    
    /**
     * Error message when authorization fails
     */
    String message() default "Access denied";

    /*
    * Validate the args
    * */
    boolean validateArgs() default false;
}
