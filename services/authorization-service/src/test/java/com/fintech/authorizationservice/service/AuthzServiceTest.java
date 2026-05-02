package com.fintech.authorizationservice.service;

import com.fintech.authorizationservice.dto.request.AuthzIntrospectRequest;
import com.fintech.authorizationservice.dto.response.AuthzIntrospectResponse;
import com.fintech.authorizationservice.entity.Role;
import com.fintech.authorizationservice.entity.RolePermission;
import com.fintech.authorizationservice.entity.Session;
import com.fintech.authorizationservice.entity.UserRole;
import com.fintech.authorizationservice.repository.*;
import com.fintech.authorizationservice.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthzServiceTest {

    @Mock
    private RoleRepository roleRepo;
    @Mock
    private RolePermissionRepository rpRepo;
    @Mock
    private FieldAccessRepository faRepo;
    @Mock
    private UserRoleRepository userRoleRepo;
    @Mock
    private ApiMethodRepository amRepo;
    @Mock
    private SessionService sessionService;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthzService authzService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ==================== introspect ====================

    @Test
    void introspect_invalidToken_returnsDenied() {
        AuthzIntrospectRequest req = new AuthzIntrospectRequest("bad-token", "/api/test", "GET", null);
        when(jwtUtil.validateToken("bad-token")).thenReturn(false);

        AuthzIntrospectResponse response = authzService.introspect(req).block();

        assertThat(response).isNotNull();
        assertThat(response.isAllowed()).isFalse();
        assertThat(response.getReason()).isEqualTo("INVALID_TOKEN");
    }

    @Test
    void introspect_noSessionId_returnsDenied() {
        AuthzIntrospectRequest req = new AuthzIntrospectRequest("token", "/api/test", "GET", null);
        when(jwtUtil.validateToken("token")).thenReturn(true);
        when(jwtUtil.getSessionIdFromToken("token")).thenReturn(null);

        AuthzIntrospectResponse response = authzService.introspect(req).block();

        assertThat(response).isNotNull();
        assertThat(response.isAllowed()).isFalse();
        assertThat(response.getReason()).isEqualTo("NO_SESSION_ID");
    }

    @Test
    void introspect_sessionInvalid_returnsDenied() {
        AuthzIntrospectRequest req = new AuthzIntrospectRequest("token", "/api/test", "GET", null);
        when(jwtUtil.validateToken("token")).thenReturn(true);
        when(jwtUtil.getSessionIdFromToken("token")).thenReturn("session-123");
        when(valueOperations.get("session:data:session-123")).thenReturn(null);
        when(sessionService.getSession("session-123")).thenReturn(Optional.empty());

        AuthzIntrospectResponse response = authzService.introspect(req).block();

        assertThat(response).isNotNull();
        assertThat(response.isAllowed()).isFalse();
        assertThat(response.getReason()).isEqualTo("SESSION_INVALID");
    }

    @Test
    void introspect_noRoles_returnsDenied() {
        AuthzIntrospectRequest req = new AuthzIntrospectRequest("token", "/api/test", "GET", null);
        when(jwtUtil.validateToken("token")).thenReturn(true);
        when(jwtUtil.getSessionIdFromToken("token")).thenReturn("session-123");
        when(valueOperations.get("session:data:session-123")).thenReturn(null);

        Session session = new Session("session-123", "user-1", System.currentTimeMillis() + 100000);
        when(sessionService.getSession("session-123")).thenReturn(Optional.of(session));
        when(userRoleRepo.findRoleIdByUserId("user-1")).thenReturn(Optional.empty());

        AuthzIntrospectResponse response = authzService.introspect(req).block();

        assertThat(response).isNotNull();
        assertThat(response.isAllowed()).isFalse();
        assertThat(response.getReason()).isEqualTo("NO_ROLES");
    }

    @Test
    void introspect_noMethodExist_returnsDenied() {
        AuthzIntrospectRequest req = new AuthzIntrospectRequest("token", "/api/unknown", "GET", null);
        when(jwtUtil.validateToken("token")).thenReturn(true);
        when(jwtUtil.getSessionIdFromToken("token")).thenReturn("session-123");
        when(valueOperations.get("session:data:session-123")).thenReturn(null);

        Session session = new Session("session-123", "user-1", System.currentTimeMillis() + 100000);
        when(sessionService.getSession("session-123")).thenReturn(Optional.of(session));
        when(userRoleRepo.findRoleIdByUserId("user-1")).thenReturn(Optional.of(1L));

        Role role = new Role("ADMIN", "desc");
        role.setRoleId(1L);
        when(roleRepo.findById(1L)).thenReturn(Optional.of(role));
        when(valueOperations.get("method:/api/unknown:GET")).thenReturn(null);
        when(amRepo.findByPathAndHttpMethod("/api/unknown", "GET")).thenReturn(Optional.empty());

        AuthzIntrospectResponse response = authzService.introspect(req).block();

        assertThat(response).isNotNull();
        assertThat(response.isAllowed()).isFalse();
        assertThat(response.getReason()).isEqualTo("NO_METHOD_EXIST");
    }

    @Test
    void introspect_accessDenied_noPermission() {
        AuthzIntrospectRequest req = new AuthzIntrospectRequest("token", "/api/test", "GET", null);
        when(jwtUtil.validateToken("token")).thenReturn(true);
        when(jwtUtil.getSessionIdFromToken("token")).thenReturn("session-123");
        when(valueOperations.get("session:data:session-123")).thenReturn(null);

        Session session = new Session("session-123", "user-1", System.currentTimeMillis() + 100000);
        when(sessionService.getSession("session-123")).thenReturn(Optional.of(session));
        when(userRoleRepo.findRoleIdByUserId("user-1")).thenReturn(Optional.of(1L));

        Role role = new Role("USER", "desc");
        role.setRoleId(1L);
        when(roleRepo.findById(1L)).thenReturn(Optional.of(role));
        when(valueOperations.get("method:/api/test:GET")).thenReturn(null);
        when(amRepo.findByPathAndHttpMethod("/api/test", "GET")).thenReturn(Optional.of(5L));
        when(rpRepo.findMatchingPermissions(1L, 5L)).thenReturn(Collections.emptyList());

        AuthzIntrospectResponse response = authzService.introspect(req).block();

        assertThat(response).isNotNull();
        assertThat(response.isAllowed()).isFalse();
        assertThat(response.getReason()).isEqualTo("ACCESS_DENIED");
    }

    @Test
    void introspect_allowed_withPermission() {
        AuthzIntrospectRequest req = new AuthzIntrospectRequest("token", "/api/test", "GET", null);
        when(jwtUtil.validateToken("token")).thenReturn(true);
        when(jwtUtil.getSessionIdFromToken("token")).thenReturn("session-123");
        when(valueOperations.get("session:data:session-123")).thenReturn(null);

        Session session = new Session("session-123", "user-1", System.currentTimeMillis() + 100000);
        when(sessionService.getSession("session-123")).thenReturn(Optional.of(session));
        when(userRoleRepo.findRoleIdByUserId("user-1")).thenReturn(Optional.of(1L));

        Role role = new Role("ADMIN", "desc");
        role.setRoleId(1L);
        when(roleRepo.findById(1L)).thenReturn(Optional.of(role));
        when(valueOperations.get("method:/api/test:GET")).thenReturn(null);
        when(amRepo.findByPathAndHttpMethod("/api/test", "GET")).thenReturn(Optional.of(5L));

        RolePermission rp = new RolePermission(1L, 5L, true);
        rp.setId(10L);
        when(rpRepo.findMatchingPermissions(1L, 5L)).thenReturn(List.of(rp));
        when(faRepo.findFieldAccessByRoleId(1L)).thenReturn(Collections.emptyList());

        AuthzIntrospectResponse response = authzService.introspect(req).block();

        assertThat(response).isNotNull();
        assertThat(response.isAllowed()).isTrue();
        assertThat(response.getUserId()).isEqualTo("user-1");
        assertThat(response.getRole()).isEqualTo("ADMIN");
    }

    // ==================== registerUserRole ====================

    @Test
    void registerUserRole_success() {
        when(userRoleRepo.existsByUserIdAndRoleName("user-1", "USER")).thenReturn(false);
        Role role = new Role("USER", "desc");
        role.setRoleId(2L);
        when(roleRepo.findByName("USER")).thenReturn(Optional.of(role));
        when(userRoleRepo.save(any(UserRole.class))).thenReturn(new UserRole("user-1", 2L));

        authzService.registerUserRole("user-1", "USER");

        verify(userRoleRepo).save(any(UserRole.class));
    }

    @Test
    void registerUserRole_alreadyExists_skips() {
        when(userRoleRepo.existsByUserIdAndRoleName("user-1", "USER")).thenReturn(true);

        authzService.registerUserRole("user-1", "USER");

        verify(userRoleRepo, never()).save(any());
    }

    @Test
    void registerUserRole_roleNotExists_createsRole() {
        when(userRoleRepo.existsByUserIdAndRoleName("user-1", "NEW_ROLE")).thenReturn(false);
        when(roleRepo.findByName("NEW_ROLE")).thenReturn(Optional.empty());
        Role newRole = new Role("NEW_ROLE", "Auto-created role for NEW_ROLE");
        newRole.setRoleId(5L);
        when(roleRepo.save(any(Role.class))).thenReturn(newRole);
        when(userRoleRepo.save(any(UserRole.class))).thenReturn(new UserRole("user-1", 5L));

        authzService.registerUserRole("user-1", "NEW_ROLE");

        verify(roleRepo).save(any(Role.class));
        verify(userRoleRepo).save(any(UserRole.class));
    }

    // ==================== updateUserRole ====================

    @Test
    void updateUserRole_success() {
        UserRole existingUserRole = new UserRole("user-1", 1L);
        when(userRoleRepo.findByUserId("user-1")).thenReturn(Optional.of(existingUserRole));
        Role newRole = new Role("ADMIN", "desc");
        newRole.setRoleId(2L);
        when(roleRepo.findByName("ADMIN")).thenReturn(Optional.of(newRole));
        when(userRoleRepo.save(any(UserRole.class))).thenReturn(existingUserRole);
        when(sessionService.getActiveSessionsForUser("user-1")).thenReturn(Collections.emptyList());

        authzService.updateUserRole("user-1", "ADMIN", "system");

        verify(userRoleRepo).save(any(UserRole.class));
    }

    @Test
    void updateUserRole_userNotFound_throwsException() {
        when(userRoleRepo.findByUserId("user-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authzService.updateUserRole("user-999", "ADMIN", "system"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User role not found");
    }

    @Test
    void updateUserRole_roleNotFound_throwsException() {
        UserRole existingUserRole = new UserRole("user-1", 1L);
        when(userRoleRepo.findByUserId("user-1")).thenReturn(Optional.of(existingUserRole));
        when(roleRepo.findByName("NONEXISTENT")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authzService.updateUserRole("user-1", "NONEXISTENT", "system"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Role not found");
    }

    // ==================== clearAuthzCache ====================

    @Test
    void clearAuthzCache_clearsAllRelatedKeys() {
        when(redisTemplate.keys("authz:introspect:session-123:*")).thenReturn(java.util.Set.of("key1"));
        when(redisTemplate.delete(anyCollection())).thenReturn(1L);

        authzService.clearAuthzCache("session-123");

        verify(redisTemplate).keys("authz:introspect:session-123:*");
        verify(redisTemplate).delete("session:data:session-123");
    }
}
