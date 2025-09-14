package com.fintech.security.aspect;

import com.fintech.security.annotation.FilterResponse;
import com.fintech.security.model.AuthorizationContext;
import com.fintech.security.service.AuthorizationService;
import com.fintech.security.util.AuthorizationContextHolder;
import com.fintech.security.util.FieldFilterUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Aspect for automatically filtering response data based on authorization context
 * Works with @FilterResponse annotation to provide seamless field-level filtering
 */
@Aspect
@Component
public class ResponseFilterAspect {

    private static final Logger logger = LoggerFactory.getLogger(ResponseFilterAspect.class);

    private final AuthorizationService authorizationService;
    private final FieldFilterUtil fieldFilterUtil;

    public ResponseFilterAspect(AuthorizationService authorizationService, FieldFilterUtil fieldFilterUtil) {
        this.authorizationService = authorizationService;
        this.fieldFilterUtil = fieldFilterUtil;
    }

    @Around("@annotation(com.fintech.security.annotation.FilterResponse)")
    public Object filterResponse(ProceedingJoinPoint joinPoint) throws Throwable {

        // Get the method and annotation
        Method method = ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature()).getMethod();
        FilterResponse filterAnnotation = method.getAnnotation(FilterResponse.class);

        if (filterAnnotation == null) {
            return joinPoint.proceed();
        }

        // Execute the original method
        Object result = joinPoint.proceed();

        // Check if we should apply filtering
        if (!shouldApplyFiltering(filterAnnotation)) {
            logger.debug("Skipping filtering for method: {} - User has full access and forceFilter is false",
                    method.getName());
            return result;
        }

        // Apply filtering based on the result type
        Object filteredResult = applyFiltering(result, filterAnnotation, method.getName());

        logger.debug("Applied response filtering for method: {} with resourceType: {}",
                method.getName(), filterAnnotation.resourceType());

        return filteredResult;
    }

    /**
     * Determine if filtering should be applied
     */
    private boolean shouldApplyFiltering(FilterResponse annotation) {
        AuthorizationContext context = AuthorizationContextHolder.getContext();
        if (context == null || !context.isAllowed()) {
            return false; // No context or not authorized - let authorization aspect handle it
        }

        // If forceFilter is true, always apply filtering
        if (annotation.forceFilter()) {
            return true;
        }

        // Check if user has full access - if so, skip filtering unless forced
        String resourceType = getResourceType(annotation);
        return !context.hasFullAccess(resourceType);
    }

    /**
     * Apply filtering to the result object
     */
    private Object applyFiltering(Object result, FilterResponse annotation, String methodName) {
        if (result == null) {
            return null;
        }

        String resourceType = getResourceType(annotation);

        try {
            // Handle ResponseEntity
            if (result instanceof ResponseEntity) {
                ResponseEntity<?> responseEntity = (ResponseEntity<?>) result;
                Object body = responseEntity.getBody();
                Object filteredBody = filterObject(body, annotation, resourceType);
                return ResponseEntity.status(responseEntity.getStatusCode())
                        .headers(responseEntity.getHeaders())
                        .body(filteredBody);
            }

            // Handle direct objects
            return filterObject(result, annotation, resourceType);

        } catch (Exception e) {
            logger.error("Error filtering response for method: {} with resourceType: {}",
                    methodName, resourceType, e);
            // Return original result if filtering fails
            return result;
        }
    }

    /**
     * Filter an object based on the annotation configuration
     */
    private Object filterObject(Object obj, FilterResponse annotation, String resourceType) {
        if (obj == null) {
            return null;
        }

        // Handle Collections (List, Set, etc.)
        if (obj instanceof Collection && annotation.filterCollectionItems()) {
            Collection<?> collection = (Collection<?>) obj;
            if (obj instanceof List) {
                return filterList((List<?>) collection, annotation, resourceType);
            } else if (obj instanceof Set) {
                return filterSet((Set<?>) collection, annotation, resourceType);
            } else {
                // Generic collection - convert to list
                List<?> list = new ArrayList<>(collection);
                return filterList(list, annotation, resourceType);
            }
        }

        // Handle Arrays
        if (obj.getClass().isArray() && annotation.filterCollectionItems()) {
            Object[] array = (Object[]) obj;
            Object[] filteredArray = new Object[array.length];
            for (int i = 0; i < array.length; i++) {
                filteredArray[i] = filterSingleObject(array[i], annotation, resourceType);
            }
            return filteredArray;
        }

        // Handle Maps
        if (obj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) obj;

            // Check if this is a wrapper map (like search results)
            if (map.containsKey("results") && map.get("results") instanceof Collection) {
                Map<String, Object> filteredMap = new HashMap<>(map);
                Object results = map.get("results");
                filteredMap.put("results", filterObject(results, annotation, resourceType));
                return filteredMap;
            }

            // Filter the map directly as field data
            return authorizationService.filterFields(map, resourceType);
        }

        // Handle single objects
        return filterSingleObject(obj, annotation, resourceType);
    }

    /**
     * Filter a single object
     */
    private Object filterSingleObject(Object obj, FilterResponse annotation, String resourceType) {
        if (obj == null) {
            return null;
        }

        // Convert to map if requested
        if (annotation.convertToMap()) {
            return fieldFilterUtil.createFilteredMap(obj, resourceType);
        }

        // Use field filter util to filter object fields based on @FieldAccessControl annotations
        return fieldFilterUtil.filterFields(obj, resourceType);
    }

    /**
     * Filter a List
     */
    private List<?> filterList(List<?> list, FilterResponse annotation, String resourceType) {
        return list.stream()
                .map(item -> filterSingleObject(item, annotation, resourceType))
                .toList();
    }

    /**
     * Filter a Set
     */
    private Set<?> filterSet(Set<?> set, FilterResponse annotation, String resourceType) {
        return set.stream()
                .map(item -> filterSingleObject(item, annotation, resourceType))
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    /**
     * Get the resource type from annotation
     */
    private String getResourceType(FilterResponse annotation) {
        if (!annotation.fieldAccessKey().isEmpty()) {
            return annotation.fieldAccessKey();
        }
        return annotation.resourceType();
    }
}