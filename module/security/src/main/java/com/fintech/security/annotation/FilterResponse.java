package com.fintech.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to automatically filter response data based on authorization context
 * Eliminates the need to manually call filtering methods in controllers
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface FilterResponse {
    
    /**
     * Resource type for field access filtering (e.g., "user", "account", "transaction")
     */
    String resourceType();
    
    /**
     * Whether to apply filtering even if user has full access
     * Default is false - users with full access bypass filtering
     */
    boolean forceFilter() default false;
    
    /**
     * Custom field access key for complex scenarios
     * If specified, will use this instead of the default resourceType
     */
    String fieldAccessKey() default "";
    
    /**
     * Whether to filter collection items individually
     * Default is true - each item in collections will be filtered
     */
    boolean filterCollectionItems() default true;
    
    /**
     * Whether to convert objects to Maps for filtering
     * Default is false - preserves object types
     */
    boolean convertToMap() default false;
}