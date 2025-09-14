package com.fintech.security.aspect;

import com.fintech.security.annotation.FieldAccessControl;
import com.fintech.security.annotation.RequireAuthorization;
import com.fintech.security.service.AuthorizationService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;


@Aspect
@Component
public class AuthorizationAspect {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationAspect.class);

    private final AuthorizationService authorizationService;

    public AuthorizationAspect(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Around("@annotation(com.fintech.security.annotation.RequireAuthorization)")
    public Object checkAuthorization(ProceedingJoinPoint joinPoint) throws Throwable {

        // Get the method and annotation
        Method method = ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature()).getMethod();
        RequireAuthorization authAnnotation = method.getAnnotation(RequireAuthorization.class);

        if (authAnnotation == null) {
            return joinPoint.proceed();
        }

        // Check if user is authorized (basic authentication check)
        if (!authorizationService.isAuthorized()) {
            logger.warn("Unauthorized access attempt to method: {}", method.getName());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authorized");
        }

        // Check role requirements (only if explicitly specified - for backward compatibility)
        String[] requiredRoles = authAnnotation.roles();
        if (requiredRoles.length > 0) {
            logger.warn("Hardcoded roles detected in method: {} - Consider using database-driven permissions instead",
                    method.getName());
            boolean hasRequiredRole = false;
            for (String role : requiredRoles) {
                if (authorizationService.hasRole(role)) {
                    hasRequiredRole = true;
                    break;
                }
            }

            if (!hasRequiredRole) {
                logger.warn("Access denied to method: {} - Required roles: {} - Current role: {}",
                        method.getName(), String.join(",", requiredRoles),
                        authorizationService.getCurrentRole());
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, authAnnotation.message());
            }
        }


        // Check custom expression (database-driven permissions)
        String expression = authAnnotation.expression();
        if (!expression.isEmpty()) {
            if (!evaluateExpression(expression, joinPoint)) {
                logger.warn("Access denied to method: {} - Expression failed: {}",
                        method.getName(), expression);
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, authAnnotation.message());
            }
        }

        logger.debug("Authorization check passed for method: {} - User: {}",
                method.getName(), authorizationService.getCurrentUserId());


        Object[] filteredArgs = joinPoint.getArgs();
        if (authAnnotation.validateArgs() && filteredArgs != null && filteredArgs.length > 0) {
            filteredArgs = filterMethodArguments(joinPoint.getArgs(), authAnnotation.resourceType());
        }

        // Proceed with filtered arguments
        return joinPoint.proceed(filteredArgs);
    }

    /**
     * Evaluate authorization expressions (database-driven permissions)
     */
    private boolean evaluateExpression(String expression, ProceedingJoinPoint joinPoint) {

        // Get the method and annotation
        Method method = ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature()).getMethod();
        RequireAuthorization authAnnotation = method.getAnnotation(RequireAuthorization.class);

        // Enhanced expression evaluation for database-driven permissions
        if (expression.equals("isAdmin()")) {
            // Check if user has full access level instead of hardcoded admin role
            return authorizationService.hasAccessLevel("full", authAnnotation.resourceType());
        }

        if (expression.equals("isAuthenticated()")) {
            return authorizationService.isAuthorized();
        }

        if (expression.startsWith("hasRole('") && expression.endsWith("')")) {
            String role = expression.substring(9, expression.length() - 2);
            return authorizationService.hasRole(role);
        }

        // Database-driven permission check
        if (expression.startsWith("hasPermission('") && expression.endsWith("')")) {
            String permission = expression.substring(15, expression.length() - 2);
            return authorizationService.hasSpecificPermission(permission);
        }

        // Full access permission check (for sensitive operations)
        if (expression.startsWith("hasFullAccess('") && expression.endsWith("')")) {
            return authorizationService.hasAccessLevel("full", authAnnotation.resourceType());
        }


        //TODO: NEED TO FIX THIS EXP

        // Field access check
        if (expression.contains("hasFieldAccess(")) {
            // Example: hasFieldAccess('user_profile', 'email')
            String[] parts = expression.substring(expression.indexOf('(') + 1, expression.lastIndexOf(')')).split(",");
            if (parts.length == 2) {
                String resourceType = parts[0].trim().replace("'", "");
                String fieldName = parts[1].trim().replace("'", "");
                return authorizationService.hasFieldAccess(resourceType, fieldName);
            }
        }

        // Default to false for unknown expressions (secure by default)
        logger.warn("Unknown authorization expression: {} - Denying access", expression);
        return false;
    }

    /**
     * Filter method arguments to remove fields that the user cannot modify
     * Creates new objects containing only authorized fields based on database permissions
     */
    private Object[] filterMethodArguments(Object[] args, String defaultResourceType) {
        if (args == null || args.length == 0) {
            return args;
        }

        List<Object> filteredArgsList = new ArrayList<>();

        for (Object arg : args) {
            if (arg == null) {
                filteredArgsList.add(null);
                continue;
            }

            // Check if the argument has @FieldAccessControl annotated fields
            if (hasFieldAccessControlAnnotations(arg)) {
                // This is a complex DTO with annotations - apply filtering
                Object filteredArg = filterObjectFields(arg, defaultResourceType);
                // Only add the argument if it has any allowed fields
                if (hasAnyAllowedFields(filteredArg)) {
                    filteredArgsList.add(filteredArg);
                } else {
                    logger.debug("Argument of type {} completely removed - no fields allowed for modification",
                            arg.getClass().getSimpleName());
                    // Don't add this argument - it's completely removed
                }
            } else {
                // Simple type (String, Long, etc.) or object without annotations
                // These are typically path parameters, IDs, etc. - include as-is
                filteredArgsList.add(arg);
                logger.debug("Argument of type {} passed through unchanged - no @FieldAccessControl annotations",
                        arg.getClass().getSimpleName());
            }
        }

        return filteredArgsList.toArray();
    }

    /**
     * Check if an object has any @FieldAccessControl annotated fields
     */
    private boolean hasFieldAccessControlAnnotations(Object obj) {
        if (obj == null) {
            return false;
        }

        Class<?> clazz = obj.getClass();

        // For primitive types, wrapper types, and common simple types - check if they need filtering
        // These are typically path parameters, IDs, etc. that should pass through unchanged
        if (clazz.isPrimitive() ||
                clazz == String.class ||
                clazz == Long.class ||
                clazz == Integer.class ||
                clazz == Boolean.class ||
                clazz == Double.class ||
                clazz == Float.class ||
                Number.class.isAssignableFrom(clazz) ||
                clazz.isEnum()) {
            return false; // These don't have @FieldAccessControl annotations
        }

        // Check if any field has @FieldAccessControl annotation
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(FieldAccessControl.class)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Filter fields in an object based on modification permissions
     */
    private Object filterObjectFields(Object obj, String defaultResourceType) {
        if (obj == null) {
            return null;
        }

        try {
            // Create a new filtered object to avoid modifying original
            Object filteredObj = createFilteredObject(obj, defaultResourceType);
            return filteredObj;

        } catch (Exception e) {
            logger.error("Error filtering object fields for argument: {}", obj.getClass().getSimpleName(), e);
            // Return original object if filtering fails (fail-safe approach)
            return obj;
        }
    }

    /**
     * Create a new object with only the fields that user can modify
     */
    private Object createFilteredObject(Object originalObj, String defaultResourceType) throws Exception {
        Class<?> objClass = originalObj.getClass();
        Object filteredObj;

        try {
            // Try to create new instance using default constructor
            filteredObj = objClass.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            logger.warn("No default constructor found for class: {}. Using reflection to create instance.", objClass.getSimpleName());
            // Fallback: try to create instance using available constructors
            try {
                // Try to find any constructor and use it with null parameters
                var constructors = objClass.getDeclaredConstructors();
                if (constructors.length > 0) {
                    var constructor = constructors[0];
                    constructor.setAccessible(true);
                    Object[] initArgs = new Object[constructor.getParameterCount()];
                    filteredObj = constructor.newInstance(initArgs);
                } else {
                    logger.error("No constructors found for class: {}. Returning original object.", objClass.getSimpleName());
                    return originalObj;
                }
            } catch (Exception ex) {
                logger.error("Cannot create filtered object for class: {}. Returning original object.", objClass.getSimpleName());
                return originalObj;
            }
        }

        Field[] fields = objClass.getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            Object fieldValue = field.get(originalObj);

            FieldAccessControl annotation = field.getAnnotation(FieldAccessControl.class);

            if (annotation != null) {
                String resourceType = annotation.resourceType().isEmpty() ?
                        defaultResourceType : annotation.resourceType();
                String fieldName = annotation.fieldName().isEmpty() ?
                        field.getName() : annotation.fieldName();

                // Check if user can modify this field
                if (authorizationService.canModifyField(resourceType, fieldName)) {
                    // User can modify - include the field
                    if (fieldValue != null && hasFieldAccessControlAnnotations(fieldValue)) {
                        // Recursively filter nested object
                        Object filteredNestedObj = filterObjectFields(fieldValue, resourceType);
                        field.set(filteredObj, filteredNestedObj);
                        logger.debug("Field modification allowed for nested field: {} (annotation fieldName: {}) of resource type: {} - applied recursive filtering",
                                field.getName(), fieldName, resourceType);
                    } else {
                        // Simple field or field without annotations - copy value
                        field.set(filteredObj, fieldValue);
                        logger.debug("Field modification allowed for field: {} (annotation fieldName: {}) of resource type: {}",
                                field.getName(), fieldName, resourceType);
                    }
                } else {
                    // User cannot modify - COMPLETELY EXCLUDE this field (leave as null/default)
                    // Do not set anything - field remains uninitialized in new object
                    logger.debug("Field modification denied for field: {} (annotation fieldName: {}) of resource type: {} - field completely excluded",
                            field.getName(), fieldName, resourceType);
                }
            } else {
                // No annotation - only include if it's a nested object with annotations
                if (fieldValue != null && hasFieldAccessControlAnnotations(fieldValue)) {
                    // Recursively filter nested object
                    Object filteredNestedObj = filterObjectFields(fieldValue, defaultResourceType);
                    field.set(filteredObj, filteredNestedObj);
                    logger.debug("Non-annotated nested object found in field: {} - applied recursive filtering", field.getName());
                } else {
                    // No annotation and no nested annotations - skip this field completely for security
                    logger.debug("Field {} has no @FieldAccessControl annotation - excluded for security", field.getName());
                }
            }
        }

        return filteredObj;
    }

    /**
     * Check if an object has any fields that were allowed to be modified
     * This determines if the filtered object should be included in arguments
     */
    private boolean hasAnyAllowedFields(Object obj) {
        if (obj == null) {
            return false;
        }

        try {
            Field[] fields = obj.getClass().getDeclaredFields();

            for (Field field : fields) {
                field.setAccessible(true);
                Object fieldValue = field.get(obj);

                // Check if this field has annotation and was set in the filtered object
                FieldAccessControl annotation = field.getAnnotation(FieldAccessControl.class);
                if (annotation != null && fieldValue != null) {
                    return true; // Found at least one allowed field with value
                }

                // Also check for non-annotated fields that might have been included
                if (annotation == null && fieldValue != null) {
                    // Non-annotated field with value - consider this as having allowed fields
                    return true;
                }

                // Also check nested objects recursively
                if (fieldValue != null && hasFieldAccessControlAnnotations(fieldValue)) {
                    if (hasAnyAllowedFields(fieldValue)) {
                        return true;
                    }
                }
            }

            return false; // No allowed fields found

        } catch (Exception e) {
            logger.warn("Error checking allowed fields for object: {}", obj.getClass().getSimpleName(), e);
            return true; // Default to true if we can't check (fail-safe)
        }
    }
}