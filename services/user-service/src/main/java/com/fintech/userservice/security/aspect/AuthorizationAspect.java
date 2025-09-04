package com.fintech.userservice.security.aspect;

import com.fintech.userservice.security.annotation.RequireAuthorization;
import com.fintech.userservice.security.service.AuthorizationService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;


@Aspect
@Component
public class AuthorizationAspect {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationAspect.class);

    private final AuthorizationService authorizationService;

    public AuthorizationAspect(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    @Around("@annotation(com.fintech.userservice.security.annotation.RequireAuthorization)")
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

        return joinPoint.proceed();
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
}
