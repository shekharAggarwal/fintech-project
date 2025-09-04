package com.fintech.authorizationservice.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.authorizationservice.dto.request.AuthzIntrospectRequest;
import com.fintech.authorizationservice.dto.response.AuthzIntrospectResponse;
import com.fintech.authorizationservice.entity.Role;
import com.fintech.authorizationservice.entity.RolePermission;
import com.fintech.authorizationservice.entity.Session;
import com.fintech.authorizationservice.entity.UserRole;
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
    private static final int CACHE_TTL_SECONDS = 300; // 5 minutes


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

            // 2. Cache lookup - fast path
            String cacheKey = AUTHZ_CACHE_PREFIX + sessionId + ":" + method + ":" + path;
            try {
                String cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    log.debug("Cache hit for key {}", cacheKey);
                    return mapper.readValue(cached, AuthzIntrospectResponse.class);
                }
            } catch (Exception e) {
                log.warn("Cache read failed for key {}", cacheKey, e);
            }

            // 3. Validate session
            Optional<Session> sessionOpt = sessionService.getSession(sessionId);
            if (sessionOpt.isEmpty()) {
                return buildDenied("SESSION_INVALID");
            }
            String userId = sessionOpt.get().getUserId();

            // 4. Load role ID
            Optional<Long> roleId = userRoleRepo.findRoleIdByUserId(userId);
            if (roleId.isEmpty()) {
                return buildDenied("NO_ROLES");
            }

            //5. Check method id - with caching improvement
            String methodCacheKey = "method:" + path + ":" + method;
            Long methodId = null;
            try {
                String cachedMethodId = redisTemplate.opsForValue().get(methodCacheKey);
                if (cachedMethodId != null) {
                    methodId = Long.valueOf(cachedMethodId);
                } else {
                    Optional<Long> methodIdOpt = amRepo.findByPathAndHttpMethod(path, method);
                    if (methodIdOpt.isEmpty()) {
                        return buildDenied("NO_METHOD_EXIST");
                    }
                    methodId = methodIdOpt.get();
                    // Cache method ID for 1 hour
                    redisTemplate.opsForValue().set(methodCacheKey, String.valueOf(methodId), Duration.ofHours(1));
                }
            } catch (Exception e) {
                log.warn("Method cache operation failed", e);
                Optional<Long> methodIdOpt = amRepo.findByPathAndHttpMethod(path, method);
                if (methodIdOpt.isEmpty()) {
                    return buildDenied("NO_METHOD_EXIST");
                }
                methodId = methodIdOpt.get();
            }

            // 6. Check permissions using role id and method id
            List<RolePermission> permissionData = rpRepo.findMatchingPermissions(roleId.get(), methodId);
            if (permissionData.isEmpty()) {
                return buildDenied("ACCESS_DENIED");
            }

            //7. get role name - with caching
            String roleName = null;
            String roleCacheKey = "role:" + roleId.get();
            try {
                String cachedRoleName = redisTemplate.opsForValue().get(roleCacheKey);
                if (cachedRoleName != null) {
                    roleName = cachedRoleName;
                } else {
                    Optional<Role> role = roleRepo.findById(roleId.get());
                    if (role.isEmpty()) {
                        return buildDenied("NO_ROLE");
                    }
                    roleName = role.get().getName();
                    // Cache role name for 1 hour
                    redisTemplate.opsForValue().set(roleCacheKey, roleName, Duration.ofHours(1));
                }
            } catch (Exception e) {
                log.warn("Role cache operation failed", e);
                Optional<Role> role = roleRepo.findById(roleId.get());
                if (role.isEmpty()) {
                    return buildDenied("NO_ROLE");
                }
                roleName = role.get().getName();
            }

            // 8. Build response
            AuthzIntrospectResponse resp = new AuthzIntrospectResponse();
            resp.setAllowed(true);
            resp.setUserId(userId);
            resp.setRole(roleName);

            // Extract permissions
            Set<String> permKeys = new HashSet<>();
            permKeys.add(path); // Add the requested path as permitted

            // 9. resource access aggregation using optimized query
            Map<String, Map<String, Object>> resourceAccess = new HashMap<>();
            List<Object[]> fieldData = faRepo.findFieldAccessByRoleId(roleId.get());

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

            resp.setPermissions(new ArrayList<>(permKeys));
            resp.setResourceAccess(resourceAccess);

            // 10. Cache result
            try {
                redisTemplate.opsForValue().set(cacheKey, mapper.writeValueAsString(resp),
                        Duration.ofSeconds(CACHE_TTL_SECONDS));
            } catch (Exception e) {
                log.warn("Cache write failed for key {}", cacheKey, e);
            }

            log.info("Authorization successful for userId={} path={} method={} roles={} response={}",
                    userId, path, method, roleName, mapper.writeValueAsString(resp));

            return resp;

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
     * Get all roles for a user
     */
    @Transactional(readOnly = true)
    public List<String> getUserRoles(String userId) {
        return userRoleRepo.findRoleNamesByUserId(userId);
    }

    /**
     * Clear authorization cache for a specific session
     * Note: This clears all path-specific caches for the session
     */
    public void clearAuthzCache(String sessionId) {
        try {
            // Clear all cache entries that start with the session prefix
            String pattern = AUTHZ_CACHE_PREFIX + sessionId + ":*";
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Cleared {} authorization cache entries for sessionId: {}", keys.size(), sessionId);
            }
        } catch (Exception e) {
            log.warn("Failed to clear authorization cache for sessionId: {}", sessionId, e);
        }
    }

    /**
     * Clear authorization cache for a specific session and path
     */
    public void clearAuthzCacheForPath(String sessionId, String method, String path) {
        try {
            String cacheKey = AUTHZ_CACHE_PREFIX + sessionId + ":" + method + ":" + path.hashCode();
            redisTemplate.delete(cacheKey);
            log.info("Cleared authorization cache for sessionId: {}, path: {}, method: {}", sessionId, path, method);
        } catch (Exception e) {
            log.warn("Failed to clear authorization cache for sessionId: {}, path: {}", sessionId, path, e);
        }
    }

    /**
     * Clear authorization cache for all sessions of a user
     */
    @Transactional(readOnly = true)
    public void clearUserAuthzCache(String userId) {
        try {
            // Get all active sessions for the user
            List<Session> userSessions = sessionService.getActiveSessionsForUser(userId);
            for (Session session : userSessions) {
                clearAuthzCache(session.getSessionId());
            }
            log.info("Cleared all authorization cache for userId: {}", userId);
        } catch (Exception e) {
            log.warn("Failed to clear authorization cache for userId: {}", userId, e);
        }
    }
}