package com.fintech.authorizationservice.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.authorizationservice.dto.request.AuthzIntrospectRequest;
import com.fintech.authorizationservice.dto.response.AuthzIntrospectResponse;
import com.fintech.authorizationservice.entity.Role;
import com.fintech.authorizationservice.entity.RolePermission;
import com.fintech.authorizationservice.entity.Session;
import com.fintech.authorizationservice.entity.UserRole;
import com.fintech.authorizationservice.model.RoleAuthzCacheData;
import com.fintech.authorizationservice.model.SessionCacheData;
import com.fintech.authorizationservice.repository.*;
import com.fintech.authorizationservice.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.*;

@Service
public class AuthzService {


    private final RoleRepository roleRepo;
    private final RolePermissionRepository rpRepo;
    private final FieldAccessRepository faRepo;
    private final UserRoleRepository userRoleRepo;
    private final ApiMethodRepository amRepo;

    private final SessionService sessionService;
    private final JwtUtil jwtUtil;


    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper mapper = new ObjectMapper();


    private static final Logger log = LoggerFactory.getLogger(AuthzService.class);
    private static final String AUTHZ_CACHE_PREFIX = "authz:introspect:";
    private static final String SESSION_CACHE_PREFIX = "session:data:";
    private static final String PERMISSION_CACHE_PREFIX = "role:authz:"; // Role-based authorization cache
    private static final int SESSION_CACHE_TTL_SECONDS = 1800; // 30 minutes


    public AuthzService(RoleRepository roleRepo, RolePermissionRepository rpRepo,
                        FieldAccessRepository faRepo, UserRoleRepository userRoleRepo, ApiMethodRepository amRepo,
                        SessionService sessionService, JwtUtil jwtUtil, RedisTemplate<String, String> redisTemplate) {
        this.roleRepo = roleRepo;
        this.rpRepo = rpRepo;
        this.faRepo = faRepo;
        this.userRoleRepo = userRoleRepo;
        this.amRepo = amRepo;
        this.sessionService = sessionService;
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    // Introspect: validate session with JWT and compose envelope
    public Mono<AuthzIntrospectResponse> introspect(AuthzIntrospectRequest req) {
        return Mono.fromCallable(() -> performIntrospectSync(req))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(ex -> log.error("Introspect error for path={} method={}", req.path(), req.method(), ex))
                .onErrorReturn(buildDenied("INTERNAL_ERROR"));
    }

    @Transactional(readOnly = true)
    protected AuthzIntrospectResponse performIntrospectSync(AuthzIntrospectRequest req) {
        try {
            String jwtToken = req.jwtToken();
            String path = req.path();
            String method = req.method();

            // 1. Validate JWT
            if (!jwtUtil.validateToken(jwtToken)) {
                return buildDenied("INVALID_TOKEN");
            }
            String sessionId = jwtUtil.getSessionIdFromToken(jwtToken);
            if (sessionId == null) {
                return buildDenied("NO_SESSION_ID");
            }

            // 2. Session-based cache lookup (new approach)
            SessionCacheData sessionData = getSessionFromCache(sessionId);
            if (sessionData != null && sessionData.isValid()) {
                log.debug("Session cache hit for sessionId: {}", sessionId);

                // Check role-based permission cache
                String permissionKey = PERMISSION_CACHE_PREFIX + sessionData.getRoleId() + ":" + method + ":" + path;
                String cachedRoleAuthz = redisTemplate.opsForValue().get(permissionKey);
                if (cachedRoleAuthz != null) {
                    try {
                        RoleAuthzCacheData roleAuthzData = mapper.readValue(cachedRoleAuthz, RoleAuthzCacheData.class);
                        log.debug("Role authorization cache hit for roleId: {}, path: {}, method: {}", sessionData.getRoleId(), path, method);

                        // Build response combining cached role data with current user data
                        AuthzIntrospectResponse response = new AuthzIntrospectResponse();
                        response.setAllowed(roleAuthzData.isAllowed());
                        response.setUserId(sessionData.getUserId());
                        response.setRole(roleAuthzData.getRole());
                        response.setPermissions(roleAuthzData.getPermissions());
                        response.setResourceAccess(roleAuthzData.getResourceAccess());
                        response.setReason(roleAuthzData.getReason());

                        return response;
                    } catch (Exception e) {
                        log.warn("Failed to deserialize cached role authorization for key: {}", permissionKey, e);
                        // Continue with normal flow if deserialization fails
                    }
                }
            }

            // 3. Validate session (if not cached or expired)
            String userId;
            Long roleId;
            String roleName;

            if (sessionData != null && !sessionData.isValid()) {
                // Session data exists but expired, clear it
                clearSessionCache(sessionId);
            }

            if (sessionData == null || !sessionData.isValid()) {
                Optional<Session> sessionOpt = sessionService.getSession(sessionId);
                if (sessionOpt.isEmpty()) {
                    return buildDenied("SESSION_INVALID");
                }
                userId = sessionOpt.get().getUserId();

                // 4. Load role ID
                Optional<Long> roleIdOpt = userRoleRepo.findRoleIdByUserId(userId);
                if (roleIdOpt.isEmpty()) {
                    return buildDenied("NO_ROLES");
                }
                roleId = roleIdOpt.get();

                // 5. Get role name
                Optional<Role> role = roleRepo.findById(roleId);
                if (role.isEmpty()) {
                    return buildDenied("NO_ROLE");
                }
                roleName = role.get().getName();

                // Cache session data for future use
                cacheSessionData(sessionId, userId, roleId, roleName);
            } else {
                // Use cached data
                userId = sessionData.getUserId();
                roleId = sessionData.getRoleId();
                roleName = sessionData.getRoleName();
            }

            // 6. Check method ID with caching
            Long methodId = getMethodIdWithCache(path, method);
            if (methodId == null) {
                return buildDenied("NO_METHOD_EXIST");
            }

            // 7. Check permissions using role id and method id
            List<RolePermission> permissionData = rpRepo.findMatchingPermissions(roleId, methodId);
            boolean hasPermission = !permissionData.isEmpty();

            // Build the appropriate response
            AuthzIntrospectResponse response = new AuthzIntrospectResponse();
            response.setUserId(userId);

            // Create role-specific cache data
            RoleAuthzCacheData roleAuthzData;
            if (hasPermission) {
                // Build role-specific data (reusable across users with same role)
                roleAuthzData = buildRoleAuthzData(roleName, roleId, path);

                // Set response data
                response.setAllowed(true);
                response.setRole(roleAuthzData.getRole());
                response.setPermissions(roleAuthzData.getPermissions());
                response.setResourceAccess(roleAuthzData.getResourceAccess());
            } else {
                // Create denied role authorization data
                roleAuthzData = new RoleAuthzCacheData(roleName, null, null, "ACCESS_DENIED");

                // Set response data
                response.setAllowed(false);
                response.setReason("ACCESS_DENIED");
                response.setRole(roleName);
            }

            // Cache the role-specific authorization data (reusable across users with same role)
            try {
                String permissionKey = PERMISSION_CACHE_PREFIX + roleId + ":" + method + ":" + path;
                String serializedRoleAuthz = mapper.writeValueAsString(roleAuthzData);
                redisTemplate.opsForValue().set(permissionKey, serializedRoleAuthz, Duration.ofMinutes(10));
                log.debug("Cached role authorization data for roleId: {}, path: {}, method: {}", roleId, path, method);
            } catch (Exception e) {
                log.warn("Failed to cache role authorization for roleId: {}, path: {}, method: {}", roleId, path, method, e);
            }

            log.info("Authorization processed for userId={} roleId={} path={} method={} allowed={}",
                    userId, roleId, path, method, response.isAllowed());

            return response;

        } catch (Exception ex) {
            log.error("Introspect error for path={} method={}", req.path(), req.method(), ex);
            return buildDenied("INTERNAL_ERROR");
        }
    }


    // Helper
    private AuthzIntrospectResponse buildDenied(String reason) {
        AuthzIntrospectResponse resp = new AuthzIntrospectResponse();
        resp.setAllowed(false);
        resp.setReason(reason);
        return resp;
    }

    /**
     * Get session data from cache
     */
    private SessionCacheData getSessionFromCache(String sessionId) {
        try {
            String cacheKey = SESSION_CACHE_PREFIX + sessionId;
            String cachedData = redisTemplate.opsForValue().get(cacheKey);
            if (cachedData != null) {
                return mapper.readValue(cachedData, SessionCacheData.class);
            }
        } catch (Exception e) {
            log.warn("Failed to read session cache for sessionId: {}", sessionId, e);
        }
        return null;
    }

    /**
     * Cache session data
     */
    private void cacheSessionData(String sessionId, String userId, Long roleId, String roleName) {
        try {
            String cacheKey = SESSION_CACHE_PREFIX + sessionId;
            long validationTime = System.currentTimeMillis() + (SESSION_CACHE_TTL_SECONDS * 1000L);
            SessionCacheData sessionData = new SessionCacheData(userId, roleId, roleName, validationTime);
            String serializedData = mapper.writeValueAsString(sessionData);
            redisTemplate.opsForValue().set(cacheKey, serializedData, Duration.ofSeconds(SESSION_CACHE_TTL_SECONDS));
            log.debug("Cached session data for sessionId: {}", sessionId);
        } catch (Exception e) {
            log.warn("Failed to cache session data for sessionId: {}", sessionId, e);
        }
    }

    /**
     * Clear session cache
     */
    private void clearSessionCache(String sessionId) {
        try {
            String cacheKey = SESSION_CACHE_PREFIX + sessionId;
            redisTemplate.delete(cacheKey);
            log.debug("Cleared session cache for sessionId: {}", sessionId);
        } catch (Exception e) {
            log.warn("Failed to clear session cache for sessionId: {}", sessionId, e);
        }
    }

    /**
     * Get method ID with caching
     */
    private Long getMethodIdWithCache(String path, String method) {
        try {
            String methodCacheKey = "method:" + path + ":" + method;
            String cachedMethodId = redisTemplate.opsForValue().get(methodCacheKey);
            if (cachedMethodId != null) {
                return Long.valueOf(cachedMethodId);
            } else {
                Optional<Long> methodIdOpt = amRepo.findByPathAndHttpMethod(path, method);
                if (methodIdOpt.isPresent()) {
                    Long methodId = methodIdOpt.get();
                    // Cache method ID for 1 hour
                    redisTemplate.opsForValue().set(methodCacheKey, String.valueOf(methodId), Duration.ofHours(1));
                    return methodId;
                }
            }
        } catch (Exception e) {
            log.warn("Method cache operation failed", e);
            Optional<Long> methodIdOpt = amRepo.findByPathAndHttpMethod(path, method);
            if (methodIdOpt.isPresent()) {
                return methodIdOpt.get();
            }
        }
        return null;
    }

    /**
     * Build role-specific authorization data that can be cached and reused across users
     */
    private RoleAuthzCacheData buildRoleAuthzData(String roleName, Long roleId, String path) {
        try {
            // Extract permissions
            Set<String> permKeys = new HashSet<>();
            permKeys.add(path); // Add the requested path as permitted

            // Resource access aggregation using optimized query
            Map<String, Map<String, Object>> resourceAccess = new HashMap<>();
            List<Object[]> fieldData = faRepo.findFieldAccessByRoleId(roleId);

            for (Object[] row : fieldData) {
                String resourceType = Optional.ofNullable((String) row[0])
                        .map(String::toLowerCase)
                        .orElse("");

                // Initialize resource type map if not exists
                resourceAccess.computeIfAbsent(resourceType, k -> {
                    Map<String, Object> resourceMap = new HashMap<>();
                    resourceMap.put("access_field", new ArrayList<String>());
                    resourceMap.put("config", new HashMap<String, Object>());
                    return resourceMap;
                });

                Map<String, Object> resourceMap = resourceAccess.get(resourceType);

                String allowedFieldsJson = (String) row[1];
                if (allowedFieldsJson != null) {
                    try {
                        @SuppressWarnings("unchecked")
                        List<String> fields = mapper.readValue(allowedFieldsJson, List.class);
                        if (fields != null && !fields.isEmpty()) {
                            @SuppressWarnings("unchecked")
                            List<String> accessFields = (List<String>) resourceMap.get("access_field");
                            accessFields.addAll(fields);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse allowed fields JSON for resource {}: {}", resourceType, allowedFieldsJson, e);
                    }
                }

                String configJson = (String) row[2];
                if (configJson != null) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> configs = mapper.readValue(configJson, Map.class);
                        if (configs != null && !configs.isEmpty()) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> resourceConfig = (Map<String, Object>) resourceMap.get("config");
                            resourceConfig.putAll(configs);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse config JSON for resource {}: {}", resourceType, configJson, e);
                    }
                }
            }

            // Deduplicate access fields for each resource type
            resourceAccess.forEach((resourceType, resourceMap) -> {
                @SuppressWarnings("unchecked")
                List<String> accessFields = (List<String>) resourceMap.get("access_field");
                resourceMap.put("access_field", new ArrayList<>(new LinkedHashSet<>(accessFields)));
            });

            return new RoleAuthzCacheData(roleName, new ArrayList<>(permKeys), resourceAccess, null);
        } catch (Exception e) {
            log.error("Failed to build role authorization data for roleId={}, roleName={}", roleId, roleName, e);
            return new RoleAuthzCacheData(roleName, null, null, "INTERNAL_ERROR");
        }
    }

    /**
     * Register a user role when a new user is created
     */
    @Transactional
    public void registerUserRole(String userId, String roleName) {
        try {
            log.info("Registering user role: userId={}, roleName={}", userId, roleName);

            // Check if user already has this role
            if (userRoleRepo.existsByUserIdAndRoleName(userId, roleName)) {
                log.warn("User already has role: userId={}, roleName={}", userId, roleName);
                return;
            }

            // Find or create the role
            Role role = roleRepo.findByName(roleName)
                    .orElseGet(() -> {
                        log.info("Creating new role: {}", roleName);
                        Role newRole = new Role(roleName, "Auto-created role for " + roleName);
                        return roleRepo.save(newRole);
                    });

            // Create user-role mapping
            UserRole userRole = new UserRole(userId, role.getRoleId());
            userRoleRepo.save(userRole);

            log.info("Successfully registered user role: userId={}, roleName={}", userId, roleName);

        } catch (Exception e) {
            log.error("Failed to register user role: userId={}, roleName={}", userId, roleName, e);
            throw new RuntimeException("Failed to register user role", e);
        }
    }

    /**
     * Update user role - used by internal services
     */
    @Transactional
    public void updateUserRole(String userId, String newRoleName, String updatedBy) {
        try {
            log.info("Updating user role: userId={}, newRole={}, updatedBy={}", userId, newRoleName, updatedBy);

            // Find the user's current role
            Optional<UserRole> existingUserRole = userRoleRepo.findByUserId(userId);
            if (existingUserRole.isEmpty()) {
                throw new RuntimeException("User role not found for userId: " + userId);
            }

            // Find the new role
            Role newRole = roleRepo.findByName(newRoleName)
                    .orElseThrow(() -> new RuntimeException("Role not found: " + newRoleName));

            // Update the user's role
            UserRole userRole = existingUserRole.get();
            Long oldRoleId = userRole.getRole();
            userRole.setRole(newRole.getRoleId());
            userRoleRepo.save(userRole);

            // Clear the authorization cache for this user
            clearUserAuthzCache(userId);

            log.info("User role updated successfully: userId={}, oldRoleId={}, newRoleId={}, updatedBy={}",
                    userId, oldRoleId, newRole.getRoleId(), updatedBy);

        } catch (Exception e) {
            log.error("Failed to update user role: userId={}, newRole={}, updatedBy={}", userId, newRoleName, updatedBy, e);
            throw new RuntimeException("Failed to update user role: " + e.getMessage(), e);
        }
    }

    /**
     * Clear authorization cache for a specific session
     * This now clears both old-style and new-style caches
     */
    public void clearAuthzCache(String sessionId) {
        try {
            // Clear old-style authorization cache entries
            String oldPattern = AUTHZ_CACHE_PREFIX + sessionId + ":*";
            Set<String> oldKeys = redisTemplate.keys(oldPattern);
            if (oldKeys != null && !oldKeys.isEmpty()) {
                redisTemplate.delete(oldKeys);
                log.info("Cleared {} old authorization cache entries for sessionId: {}", oldKeys.size(), sessionId);
            }

            // Clear new-style session cache
            clearSessionCache(sessionId);

            log.info("Cleared all authorization cache for sessionId: {}", sessionId);
        } catch (Exception e) {
            log.warn("Failed to clear authorization cache for sessionId: {}", sessionId, e);
        }
    }

    /**
     * Clear authorization cache for all sessions of a user
     */
    @Transactional(readOnly = true)
    public void clearUserAuthzCache(String userId) {
        try {
            // Get all active sessions for the user and clear their caches
            List<Session> userSessions = sessionService.getActiveSessionsForUser(userId);
            for (Session session : userSessions) {
                clearAuthzCache(session.getSessionId());
            }

            // Also clear role authorization cache for user's role
            Optional<Long> roleIdOpt = userRoleRepo.findRoleIdByUserId(userId);
            if (roleIdOpt.isPresent()) {
                String permissionPattern = PERMISSION_CACHE_PREFIX + roleIdOpt.get() + ":*";
                Set<String> permissionKeys = redisTemplate.keys(permissionPattern);
                if (permissionKeys != null && !permissionKeys.isEmpty()) {
                    redisTemplate.delete(permissionKeys);
                    log.info("Cleared {} role authorization cache entries for roleId: {}", permissionKeys.size(), roleIdOpt.get());
                }
            }

            log.info("Cleared all authorization cache for userId: {}", userId);
        } catch (Exception e) {
            log.warn("Failed to clear authorization cache for userId: {}", userId, e);
        }
    }
}