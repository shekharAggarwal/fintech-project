package com.fintech.security.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fintech.security.annotation.FieldAccessControl;
import com.fintech.security.service.AuthorizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility for filtering object fields based on @FieldAccessControl annotations
 * <p>
 * Filtering Logic:
 * 1. Only fields annotated with @FieldAccessControl are processed
 * 2. Non-annotated fields are completely removed from the response
 * 3. For annotated fields: uses annotation.fieldName() to check against authorization context
 * 4. If access is granted: field is included in response with original value
 * 5. If access is denied:
 * - sensitive=true: field is removed completely
 * - sensitive=false: field is replaced with redactedValue
 */
@Component
public class FieldFilterUtil {

    private static final Logger logger = LoggerFactory.getLogger(FieldFilterUtil.class);

    private final AuthorizationService authorizationService;
    private final ObjectMapper objectMapper;

    public FieldFilterUtil(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Filter object fields based on @FieldAccessControl annotations only
     * Only fields annotated with @FieldAccessControl are processed
     * Non-annotated fields are removed completely from the response
     */
    public Object filterFields(Object object, String defaultResourceType) {
        if (object == null) {
            return null;
        }

        try {
            // Convert object to map for easier manipulation
            @SuppressWarnings("unchecked")
            Map<String, Object> objectMap = objectMapper.convertValue(object, Map.class);

            // Filter fields based on annotations only
            Map<String, Object> filteredMap = filterAnnotatedFields(object.getClass(), objectMap, defaultResourceType);

            return filteredMap;
           /* // Convert back to original type
            @SuppressWarnings("unchecked")
            Class<T> clazz = (Class<T>) object.getClass();
            return objectMapper.convertValue(filteredMap, clazz);*/

        } catch (Exception e) {
            logger.error("Error filtering fields for object: {}", object.getClass().getSimpleName(), e);
            return object; // Return original object if filtering fails
        }
    }

    /**
     * Filter fields based on @FieldAccessControl annotations only
     * Only processes fields that have the @FieldAccessControl annotation
     * Non-annotated fields are completely removed from the response
     */
    private Map<String, Object> filterAnnotatedFields(Class<?> originalClass, Map<String, Object> objectMap,
                                                      String defaultResourceType) {
        Map<String, Object> filteredMap = new HashMap<>();

        // Get field annotations from the original class
        Field[] fields = originalClass.getDeclaredFields();

        for (Field field : fields) {
            FieldAccessControl annotation = field.getAnnotation(FieldAccessControl.class);

            // Only process fields that have @FieldAccessControl annotation
            if (annotation != null) {
                String resourceType = annotation.resourceType().isEmpty() ?
                        defaultResourceType : annotation.resourceType();
                String fieldName = annotation.fieldName().isEmpty() ?
                        field.getName() : annotation.fieldName();

                // Check if user has access to this field based on annotation fieldName
                if (authorizationService.hasFieldAccess(resourceType, fieldName)) {
                    // Field is allowed - include it in the response
                    if (objectMap.containsKey(field.getName())) {
                        filteredMap.put(field.getName(), objectMap.get(field.getName()));
                        logger.debug("Field access granted for field: {} (annotation fieldName: {}) of resource type: {}",
                                field.getName(), fieldName, resourceType);
                    }
                } else {
                    // Field access denied - handle based on sensitivity
                    if (!annotation.sensitive()) {
                        // Non-sensitive field - replace with type-appropriate redacted value
                        filteredMap.put(field.getName(), annotation.redactedValue());
                        logger.debug("Field access denied for non-sensitive field: {} (annotation fieldName: {}) - replaced with redacted value: {}",
                                field.getName(), fieldName, annotation.redactedValue());
                    } else {
                        // Sensitive field - remove completely
                        logger.debug("Field access denied for sensitive field: {} (annotation fieldName: {}) - removed completely",
                                field.getName(), fieldName);
                    }
                }
            }
            // Non-annotated fields are completely ignored (removed from response)
        }

        return filteredMap;
    }

    /**
     * Create a filtered copy of an object with only annotated fields
     * Uses the same annotation-based filtering logic
     */
    public Map<String, Object> createFilteredMap(Object object, String resourceType) {
        if (object == null) {
            return new HashMap<>();
        }

        // Convert object to map
        @SuppressWarnings("unchecked")
        Map<String, Object> objectMap = objectMapper.convertValue(object, Map.class);

        // Filter using annotation-based logic
        return filterAnnotatedFields(object.getClass(), objectMap, resourceType);
    }


}