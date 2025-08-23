package com.fintech.authorizationservice.service;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.authorizationservice.dto.request.AuthzIntrospectRequest;
import com.fintech.authorizationservice.dto.response.AuthzIntrospectResponse;
import com.fintech.authorizationservice.entity.FieldAccess;
import com.fintech.authorizationservice.entity.Role;
import com.fintech.authorizationservice.entity.RolePermission;
import com.fintech.authorizationservice.entity.UserRole;
import com.fintech.authorizationservice.repository.FieldAccessRepository;
import com.fintech.authorizationservice.repository.RolePermissionRepository;
import com.fintech.authorizationservice.repository.RoleRepository;
import com.fintech.authorizationservice.repository.UserRoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AuthzService {
    private final RestTemplate restTemplate;
    private final RoleRepository roleRepo;
    private final RolePermissionRepository rpRepo;
    private final FieldAccessRepository faRepo;
    private final UserRoleRepository userRoleRepo;
    private final ObjectMapper mapper = new ObjectMapper();
    private static final Logger log = LoggerFactory.getLogger(AuthzService.class);

    public AuthzService(RestTemplate restTemplate, RoleRepository roleRepo, RolePermissionRepository rpRepo, 
                       FieldAccessRepository faRepo, UserRoleRepository userRoleRepo) {
        this.restTemplate = restTemplate;
        this.roleRepo = roleRepo;
        this.rpRepo = rpRepo;
        this.faRepo = faRepo;
        this.userRoleRepo = userRoleRepo;
    }

    // Introspect: validate session with Auth and compose envelope
    public Mono<AuthzIntrospectResponse> introspect(AuthzIntrospectRequest req) {
        try {
            // Prepare the request payload
            Map<String, String> requestBody = Map.of("sessionId", req.jwtToken());

            // Make the POST request using RestTemplate
            Map<String, Object> authResp = restTemplate.postForObject(
                    "/auth/introspect",
                    requestBody,
                    Map.class
            );

            // Process the response
            boolean active = Boolean.TRUE.equals(authResp.get("active"));
            AuthzIntrospectResponse out = new AuthzIntrospectResponse();
            out.setAllowed(false);
            if (!active) {
                out.setReason("SESSION_INVALID");
                return Mono.just(out);
            }

            String userId = (String) authResp.get("userId");
            List<String> rolesFromAuth = (List<String>) authResp.getOrDefault("roles", Collections.emptyList());

            // Load Role entities from DB for the given names (roles may be dynamic)
            List<Role> roles = rolesFromAuth.stream()
                    .map(name -> roleRepo.findByName(name).orElseGet(() -> new Role(name, "temp")))
                    .collect(Collectors.toList());

            // Load rolePermissions
            List<RolePermission> rpList = rpRepo.findByRoleIn(roles);

            // Merge configs and permissions
            Set<String> permKeys = new HashSet<>();
            Map<String, Object> limits = new HashMap<>();
            for (RolePermission rp : rpList) {
                // Use the ApiMethod to get the permission key/path
                if (rp.getApiMethodId() != null) {
                    permKeys.add(rp.getApiMethodId().getPath());
                }
                
                // For now, use limitType and limitValue from RolePermission
                if (rp.getLimitType() != null && rp.getLimitValue() != null) {
                    limits.put(rp.getLimitType(), rp.getLimitValue());
                }
            }

            // Field access aggregation
            List<FieldAccess> faList = faRepo.findByRoleIn(roles);
            Map<String, LinkedHashSet<String>> fieldMap = new HashMap<>();
            for (FieldAccess fa : faList) {
                String resource = fa.getResourceType().toLowerCase();
                List<String> arr = fa.getAllowedFields() == null ? List.of() : fa.getAllowedFields();
                fieldMap.computeIfAbsent(resource, k -> new LinkedHashSet<>()).addAll(arr);
            }
            Map<String, List<String>> fieldAccess = new HashMap<>();
            for (Map.Entry<String, LinkedHashSet<String>> e : fieldMap.entrySet())
                fieldAccess.put(e.getKey(), new ArrayList<>(e.getValue()));

            out.setAllowed(true);
            out.setUserId(userId);
            out.setRoles(rolesFromAuth);
            out.setPermissions(new ArrayList<>(permKeys));
            out.setLimits(limits);
            out.setFieldAccess(fieldAccess);
            out.setPolicyVersion("pv-" + System.currentTimeMillis());
            out.setCacheTtlSeconds(5);

            return Mono.just(out);
        } catch (Exception ex) {
            log.error("Auth introspect failed", ex);
            AuthzIntrospectResponse r = new AuthzIntrospectResponse();
            r.setAllowed(false);
            r.setReason("AUTH_DOWN");
            return Mono.just(r);
        }
    }

    /**
     * Register a user role when a new user is created
     */
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
            UserRole userRole = new UserRole(userId, role);
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
    public List<String> getUserRoles(String userId) {
        return userRoleRepo.findRoleNamesByUserId(userId);
    }
}