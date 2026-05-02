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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RolePermissionRepository rolePermissionRepository;
    @Mock
    private FieldAccessRepository fieldAccessRepository;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private ApiMethodRepository apiMethodRepository;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private AdminEventPublisher eventPublisher;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AdminService adminService;

    private Role sampleRole;

    @BeforeEach
    void setUp() {
        sampleRole = new Role("ADMIN", "Administrator role");
        sampleRole.setRoleId(1L);
    }

    // ==================== ROLE CRUD ====================

    @Test
    void createRole_success() {
        CreateRoleRequest request = new CreateRoleRequest("ADMIN", "Administrator role");
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenReturn(sampleRole);

        RoleResponse response = adminService.createRole(request);

        assertThat(response.getRoleId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("ADMIN");
        assertThat(response.getDescription()).isEqualTo("Administrator role");
        verify(eventPublisher).publishRoleCreated(1L, "ADMIN");
    }

    @Test
    void createRole_duplicateName_throwsException() {
        CreateRoleRequest request = new CreateRoleRequest("ADMIN", "desc");
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(sampleRole));

        assertThatThrownBy(() -> adminService.createRole(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void getAllRoles_returnsList() {
        when(roleRepository.findAll()).thenReturn(List.of(sampleRole));

        List<RoleResponse> roles = adminService.getAllRoles();

        assertThat(roles).hasSize(1);
        assertThat(roles.get(0).getName()).isEqualTo("ADMIN");
    }

    @Test
    void getAllRoles_pageable_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Role> page = new PageImpl<>(List.of(sampleRole), pageable, 1);
        when(roleRepository.findAll(pageable)).thenReturn(page);

        Page<RoleResponse> result = adminService.getAllRoles(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("ADMIN");
    }

    @Test
    void getRoleById_found() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(sampleRole));

        RoleResponse response = adminService.getRoleById(1L);

        assertThat(response.getName()).isEqualTo("ADMIN");
    }

    @Test
    void getRoleById_notFound_throwsException() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.getRoleById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateRole_success() {
        UpdateRoleRequest request = new UpdateRoleRequest("SUPER_ADMIN", "Updated description");
        when(roleRepository.findById(1L)).thenReturn(Optional.of(sampleRole));
        when(roleRepository.findByName("SUPER_ADMIN")).thenReturn(Optional.empty());

        Role updatedRole = new Role("SUPER_ADMIN", "Updated description");
        updatedRole.setRoleId(1L);
        when(roleRepository.save(any(Role.class))).thenReturn(updatedRole);
        lenient().when(redisTemplate.delete(anyString())).thenReturn(true);
        lenient().when(redisTemplate.keys(anyString())).thenReturn(Collections.emptySet());

        RoleResponse response = adminService.updateRole(1L, request);

        assertThat(response.getName()).isEqualTo("SUPER_ADMIN");
        verify(eventPublisher).publishRoleUpdated(1L, "SUPER_ADMIN");
    }

    @Test
    void updateRole_duplicateName_throwsException() {
        Role existingOther = new Role("OTHER", "other");
        existingOther.setRoleId(2L);
        UpdateRoleRequest request = new UpdateRoleRequest("OTHER", null);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(sampleRole));
        when(roleRepository.findByName("OTHER")).thenReturn(Optional.of(existingOther));

        assertThatThrownBy(() -> adminService.updateRole(1L, request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void deleteRole_success() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(sampleRole));
        when(userRoleRepository.existsByRoleId(1L)).thenReturn(false);
        when(fieldAccessRepository.findAllByRole(1L)).thenReturn(Collections.emptyList());
        lenient().when(redisTemplate.delete(anyString())).thenReturn(true);
        lenient().when(redisTemplate.keys(anyString())).thenReturn(Collections.emptySet());

        adminService.deleteRole(1L);

        verify(roleRepository).delete(sampleRole);
        verify(rolePermissionRepository).deleteByRole(1L);
        verify(eventPublisher).publishRoleDeleted(1L, "ADMIN");
    }

    @Test
    void deleteRole_inUse_throwsException() {
        when(roleRepository.findById(1L)).thenReturn(Optional.of(sampleRole));
        when(userRoleRepository.existsByRoleId(1L)).thenReturn(true);

        assertThatThrownBy(() -> adminService.deleteRole(1L))
                .isInstanceOf(ResourceInUseException.class);
    }

    @Test
    void deleteRole_notFound_throwsException() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.deleteRole(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ==================== PERMISSION CRUD ====================

    @Test
    void createPermission_success() {
        CreatePermissionRequest request = new CreatePermissionRequest(1L, 10L, true);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(sampleRole));
        when(apiMethodRepository.findById(10L)).thenReturn(Optional.of(new com.fintech.authorizationservice.entity.ApiMethod()));
        when(rolePermissionRepository.existsByRoleAndApiMethodId(1L, 10L)).thenReturn(false);

        RolePermission saved = new RolePermission(1L, 10L, true);
        saved.setId(100L);
        when(rolePermissionRepository.save(any(RolePermission.class))).thenReturn(saved);
        lenient().when(redisTemplate.delete(anyString())).thenReturn(true);
        lenient().when(redisTemplate.keys(anyString())).thenReturn(Collections.emptySet());

        PermissionResponse response = adminService.createPermission(request);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getRoleId()).isEqualTo(1L);
        assertThat(response.isAllowed()).isTrue();
        verify(eventPublisher).publishPermissionChanged(1L, 100L, "CREATED");
    }

    @Test
    void createPermission_roleNotFound_throwsException() {
        CreatePermissionRequest request = new CreatePermissionRequest(99L, 10L, true);
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.createPermission(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createPermission_duplicate_throwsException() {
        CreatePermissionRequest request = new CreatePermissionRequest(1L, 10L, true);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(sampleRole));
        when(apiMethodRepository.findById(10L)).thenReturn(Optional.of(new com.fintech.authorizationservice.entity.ApiMethod()));
        when(rolePermissionRepository.existsByRoleAndApiMethodId(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> adminService.createPermission(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void getAllPermissions_returnsList() {
        RolePermission rp = new RolePermission(1L, 10L, true);
        rp.setId(1L);
        when(rolePermissionRepository.findAll()).thenReturn(List.of(rp));

        List<PermissionResponse> result = adminService.getAllPermissions();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isAllowed()).isTrue();
    }

    @Test
    void getAllPermissions_pageable_returnsPage() {
        RolePermission rp = new RolePermission(1L, 10L, true);
        rp.setId(1L);
        Pageable pageable = PageRequest.of(0, 10);
        Page<RolePermission> page = new PageImpl<>(List.of(rp), pageable, 1);
        when(rolePermissionRepository.findAll(pageable)).thenReturn(page);

        Page<PermissionResponse> result = adminService.getAllPermissions(pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void updatePermission_success() {
        RolePermission existing = new RolePermission(1L, 10L, false);
        existing.setId(5L);
        UpdatePermissionRequest request = new UpdatePermissionRequest(true);
        when(rolePermissionRepository.findById(5L)).thenReturn(Optional.of(existing));

        RolePermission updated = new RolePermission(1L, 10L, true);
        updated.setId(5L);
        when(rolePermissionRepository.save(any(RolePermission.class))).thenReturn(updated);
        lenient().when(redisTemplate.delete(anyString())).thenReturn(true);
        lenient().when(redisTemplate.keys(anyString())).thenReturn(Collections.emptySet());

        PermissionResponse response = adminService.updatePermission(5L, request);

        assertThat(response.isAllowed()).isTrue();
        verify(eventPublisher).publishPermissionChanged(1L, 5L, "UPDATED");
    }

    @Test
    void updatePermission_notFound_throwsException() {
        when(rolePermissionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.updatePermission(99L, new UpdatePermissionRequest(true)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deletePermission_success() {
        RolePermission existing = new RolePermission(1L, 10L, true);
        existing.setId(5L);
        when(rolePermissionRepository.findById(5L)).thenReturn(Optional.of(existing));
        lenient().when(redisTemplate.delete(anyString())).thenReturn(true);
        lenient().when(redisTemplate.keys(anyString())).thenReturn(Collections.emptySet());

        adminService.deletePermission(5L);

        verify(rolePermissionRepository).delete(existing);
        verify(eventPublisher).publishPermissionChanged(1L, 5L, "DELETED");
    }

    // ==================== FIELD ACCESS CRUD ====================

    @Test
    void createFieldAccess_success() {
        CreateFieldAccessRequest request = new CreateFieldAccessRequest(1L, "USER", "[\"name\",\"email\"]", "{\"mask\":true}");
        when(roleRepository.findById(1L)).thenReturn(Optional.of(sampleRole));
        when(fieldAccessRepository.existsByRoleAndResourceType(1L, "USER")).thenReturn(false);

        FieldAccess saved = new FieldAccess(1L, "USER", "[\"name\",\"email\"]", "{\"mask\":true}");
        saved.setId(50L);
        when(fieldAccessRepository.save(any(FieldAccess.class))).thenReturn(saved);
        lenient().when(redisTemplate.delete(anyString())).thenReturn(true);
        lenient().when(redisTemplate.keys(anyString())).thenReturn(Collections.emptySet());

        FieldAccessResponse response = adminService.createFieldAccess(request);

        assertThat(response.getId()).isEqualTo(50L);
        assertThat(response.getResourceType()).isEqualTo("USER");
    }

    @Test
    void createFieldAccess_roleNotFound_throwsException() {
        CreateFieldAccessRequest request = new CreateFieldAccessRequest(99L, "USER", null, null);
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.createFieldAccess(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createFieldAccess_duplicate_throwsException() {
        CreateFieldAccessRequest request = new CreateFieldAccessRequest(1L, "USER", null, null);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(sampleRole));
        when(fieldAccessRepository.existsByRoleAndResourceType(1L, "USER")).thenReturn(true);

        assertThatThrownBy(() -> adminService.createFieldAccess(request))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void getFieldAccessByRole_success() {
        FieldAccess fa = new FieldAccess(1L, "USER", "[\"name\"]", null);
        fa.setId(10L);
        when(roleRepository.findById(1L)).thenReturn(Optional.of(sampleRole));
        when(fieldAccessRepository.findAllByRole(1L)).thenReturn(List.of(fa));

        List<FieldAccessResponse> result = adminService.getFieldAccessByRole(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getResourceType()).isEqualTo("USER");
    }

    @Test
    void getFieldAccessByRole_roleNotFound_throwsException() {
        when(roleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.getFieldAccessByRole(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateFieldAccess_success() {
        FieldAccess existing = new FieldAccess(1L, "USER", "[\"name\"]", null);
        existing.setId(10L);
        UpdateFieldAccessRequest request = new UpdateFieldAccessRequest("[\"name\",\"email\"]", "{\"mask\":false}");

        when(fieldAccessRepository.findById(10L)).thenReturn(Optional.of(existing));
        FieldAccess updated = new FieldAccess(1L, "USER", "[\"name\",\"email\"]", "{\"mask\":false}");
        updated.setId(10L);
        when(fieldAccessRepository.save(any(FieldAccess.class))).thenReturn(updated);
        lenient().when(redisTemplate.delete(anyString())).thenReturn(true);
        lenient().when(redisTemplate.keys(anyString())).thenReturn(Collections.emptySet());

        FieldAccessResponse response = adminService.updateFieldAccess(10L, request);

        assertThat(response.getAllowedFields()).isEqualTo("[\"name\",\"email\"]");
        assertThat(response.getConfig()).isEqualTo("{\"mask\":false}");
    }

    @Test
    void updateFieldAccess_notFound_throwsException() {
        when(fieldAccessRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.updateFieldAccess(99L, new UpdateFieldAccessRequest(null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteFieldAccess_success() {
        FieldAccess existing = new FieldAccess(1L, "USER", "[\"name\"]", null);
        existing.setId(10L);
        when(fieldAccessRepository.findById(10L)).thenReturn(Optional.of(existing));
        lenient().when(redisTemplate.delete(anyString())).thenReturn(true);
        lenient().when(redisTemplate.keys(anyString())).thenReturn(Collections.emptySet());

        adminService.deleteFieldAccess(10L);

        verify(fieldAccessRepository).delete(existing);
    }

    @Test
    void deleteFieldAccess_notFound_throwsException() {
        when(fieldAccessRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.deleteFieldAccess(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
