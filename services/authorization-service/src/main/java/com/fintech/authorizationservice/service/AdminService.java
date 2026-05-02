package com.fintech.authorizationservice.service;

import com.fintech.authorizationservice.dto.request.admin.*;
import com.fintech.authorizationservice.dto.response.admin.*;
import com.fintech.authorizationservice.entity.FieldAccess;
import com.fintech.authorizationservice.entity.Role;
import com.fintech.authorizationservice.entity.RolePermission;
import com.fintech.authorizationservice.exception.DuplicateResourceException;
import com.fintech.authorizationservice.exception.ResourceInUseException;
import com.fintech.authorizationservice.exception.ResourceNotFoundException;
import com.fintech.authorizationservice.messaging.AdminEventPublisher;
import com.fintech.authorizationservice.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);
    private static final String PERMISSION_CACHE_PREFIX = "role:authz:";
    private static final String AUTHZ_CACHE_PREFIX = "authz:introspect:";

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final FieldAccessRepository fieldAccessRepository;
    private final UserRoleRepository userRoleRepository;
    private final ApiMethodRepository apiMethodRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final AdminEventPublisher eventPublisher;

    public AdminService(RoleRepository roleRepository,
                        RolePermissionRepository rolePermissionRepository,
                        FieldAccessRepository fieldAccessRepository,
                        UserRoleRepository userRoleRepository,
                        ApiMethodRepository apiMethodRepository,
                        RedisTemplate<String, String> redisTemplate,
                        AdminEventPublisher eventPublisher) {
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.fieldAccessRepository = fieldAccessRepository;
        this.userRoleRepository = userRoleRepository;
        this.apiMethodRepository = apiMethodRepository;
        this.redisTemplate = redisTemplate;
        this.eventPublisher = eventPublisher;
    }

    // ==================== ROLE CRUD ====================

    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        roleRepository.findByName(request.getName()).ifPresent(existing -> {
            throw new DuplicateResourceException("Role", "name", request.getName());
        });

        Role role = new Role(request.getName(), request.getDescription());
        role = roleRepository.save(role);

        log.info("Created role: id={}, name={}", role.getRoleId(), role.getName());
        eventPublisher.publishRoleCreated(role.getRoleId(), role.getName());

        return RoleResponse.from(role);
    }

    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(RoleResponse::from)
                .collect(Collectors.toList());
    }

    public Page<RoleResponse> getAllRoles(Pageable pageable) {
        return roleRepository.findAll(pageable).map(RoleResponse::from);
    }

    public RoleResponse getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));
        return RoleResponse.from(role);
    }

    @Transactional
    public RoleResponse updateRole(Long id, UpdateRoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));

        if (request.getName() != null && !request.getName().equals(role.getName())) {
            roleRepository.findByName(request.getName()).ifPresent(existing -> {
                throw new DuplicateResourceException("Role", "name", request.getName());
            });
            role.setName(request.getName());
        }

        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }

        role = roleRepository.save(role);
        invalidateCacheForRole(id);

        log.info("Updated role: id={}, name={}", role.getRoleId(), role.getName());
        eventPublisher.publishRoleUpdated(role.getRoleId(), role.getName());

        return RoleResponse.from(role);
    }

    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", id));

        if (userRoleRepository.existsByRoleId(id)) {
            throw new ResourceInUseException("Role", id, "Role is assigned to one or more users");
        }

        // Clean up related permissions and field access
        rolePermissionRepository.deleteByRole(id);
        fieldAccessRepository.findAllByRole(id).forEach(fa -> fieldAccessRepository.delete(fa));

        roleRepository.delete(role);
        invalidateCacheForRole(id);

        log.info("Deleted role: id={}, name={}", id, role.getName());
        eventPublisher.publishRoleDeleted(id, role.getName());
    }

    // ==================== PERMISSION CRUD ====================

    @Transactional
    public PermissionResponse createPermission(CreatePermissionRequest request) {
        // Validate role exists
        roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", request.getRoleId()));

        // Validate API method exists
        apiMethodRepository.findById(request.getApiMethodId())
                .orElseThrow(() -> new ResourceNotFoundException("ApiMethod", "id", request.getApiMethodId()));

        // Check for duplicate
        if (rolePermissionRepository.existsByRoleAndApiMethodId(request.getRoleId(), request.getApiMethodId())) {
            throw new DuplicateResourceException("Permission", "roleId+apiMethodId",
                    request.getRoleId() + "+" + request.getApiMethodId());
        }

        RolePermission permission = new RolePermission(request.getRoleId(), request.getApiMethodId(), request.isAllowed());
        permission = rolePermissionRepository.save(permission);

        invalidateCacheForRole(request.getRoleId());

        log.info("Created permission: id={}, roleId={}, apiMethodId={}", permission.getId(), request.getRoleId(), request.getApiMethodId());
        eventPublisher.publishPermissionChanged(request.getRoleId(), permission.getId(), "CREATED");

        return PermissionResponse.from(permission);
    }

    public List<PermissionResponse> getAllPermissions() {
        return rolePermissionRepository.findAll().stream()
                .map(PermissionResponse::from)
                .collect(Collectors.toList());
    }

    public Page<PermissionResponse> getAllPermissions(Pageable pageable) {
        return rolePermissionRepository.findAll(pageable).map(PermissionResponse::from);
    }

    @Transactional
    public PermissionResponse updatePermission(Long id, UpdatePermissionRequest request) {
        RolePermission permission = rolePermissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", id));

        if (request.getAllowed() != null) {
            permission.setAllowed(request.getAllowed());
        }

        permission = rolePermissionRepository.save(permission);
        invalidateCacheForRole(permission.getRole());

        log.info("Updated permission: id={}, allowed={}", id, permission.isAllowed());
        eventPublisher.publishPermissionChanged(permission.getRole(), id, "UPDATED");

        return PermissionResponse.from(permission);
    }

    @Transactional
    public void deletePermission(Long id) {
        RolePermission permission = rolePermissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", id));

        Long roleId = permission.getRole();
        rolePermissionRepository.delete(permission);
        invalidateCacheForRole(roleId);

        log.info("Deleted permission: id={}, roleId={}", id, roleId);
        eventPublisher.publishPermissionChanged(roleId, id, "DELETED");
    }

    // ==================== FIELD ACCESS CRUD ====================

    @Transactional
    public FieldAccessResponse createFieldAccess(CreateFieldAccessRequest request) {
        // Validate role exists
        roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", request.getRoleId()));

        // Check for duplicate
        if (fieldAccessRepository.existsByRoleAndResourceType(request.getRoleId(), request.getResourceType())) {
            throw new DuplicateResourceException("FieldAccess", "roleId+resourceType",
                    request.getRoleId() + "+" + request.getResourceType());
        }

        FieldAccess fieldAccess = new FieldAccess(request.getRoleId(), request.getResourceType(),
                request.getAllowedFields(), request.getConfig());
        fieldAccess = fieldAccessRepository.save(fieldAccess);

        invalidateCacheForRole(request.getRoleId());
        log.info("Created field access: id={}, roleId={}, resourceType={}", fieldAccess.getId(), request.getRoleId(), request.getResourceType());

        return FieldAccessResponse.from(fieldAccess);
    }

    public List<FieldAccessResponse> getFieldAccessByRole(Long roleId) {
        // Validate role exists
        roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));

        return fieldAccessRepository.findAllByRole(roleId).stream()
                .map(FieldAccessResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public FieldAccessResponse updateFieldAccess(Long id, UpdateFieldAccessRequest request) {
        FieldAccess fieldAccess = fieldAccessRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FieldAccess", "id", id));

        if (request.getAllowedFields() != null) {
            fieldAccess.setAllowedFields(request.getAllowedFields());
        }
        if (request.getConfig() != null) {
            fieldAccess.setConfig(request.getConfig());
        }

        fieldAccess = fieldAccessRepository.save(fieldAccess);
        invalidateCacheForRole(fieldAccess.getRole());

        log.info("Updated field access: id={}", id);
        return FieldAccessResponse.from(fieldAccess);
    }

    @Transactional
    public void deleteFieldAccess(Long id) {
        FieldAccess fieldAccess = fieldAccessRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FieldAccess", "id", id));

        Long roleId = fieldAccess.getRole();
        fieldAccessRepository.delete(fieldAccess);
        invalidateCacheForRole(roleId);

        log.info("Deleted field access: id={}, roleId={}", id, roleId);
    }

    // ==================== CACHE INVALIDATION ====================

    private void invalidateCacheForRole(Long roleId) {
        try {
            // Invalidate role authorization cache
            String roleKey = PERMISSION_CACHE_PREFIX + roleId;
            redisTemplate.delete(roleKey);

            // Invalidate all introspect caches that might reference this role
            Set<String> introspectKeys = redisTemplate.keys(AUTHZ_CACHE_PREFIX + "*");
            if (introspectKeys != null && !introspectKeys.isEmpty()) {
                redisTemplate.delete(introspectKeys);
            }

            log.debug("Invalidated cache for roleId={}", roleId);
        } catch (Exception e) {
            log.warn("Failed to invalidate cache for roleId={}: {}", roleId, e.getMessage());
        }
    }
}
