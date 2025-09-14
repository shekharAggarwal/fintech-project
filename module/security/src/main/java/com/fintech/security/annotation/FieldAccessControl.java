package com.fintech.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark fields that should be included in filtered responses
 * Only fields with this annotation will be processed during filtering
 * Non-annotated fields are completely removed from responses
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface FieldAccessControl {

    /**
     * Resource type for this field (e.g., "user", "account", "transaction")
     */
    String resourceType();

    /**
     * Field name for access control (defaults to field/property name)
     * This is the name used to check against authorization context access_field array
     */
    String fieldName() default "";

    /**
     * Whether this field is sensitive and should be redacted when access is denied
     * If true, field will be replaced with redactedValue when access is denied
     * If false, field will be removed completely when access is denied
     */
    boolean sensitive() default false;

    /**
     * Value to show when access is denied to sensitive fields
     * Only used when sensitive=true and access is denied
     */
    String redactedValue() default "***";
}