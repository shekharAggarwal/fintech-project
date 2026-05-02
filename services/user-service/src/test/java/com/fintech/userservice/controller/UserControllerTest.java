package com.fintech.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.security.service.AuthorizationService;
import com.fintech.userservice.dto.request.UpdateUserRequest;
import com.fintech.userservice.entity.UserProfile;
import com.fintech.userservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthorizationService authorizationService;

    private UserProfile sampleProfile;

    @BeforeEach
    void setUp() throws Exception {
        sampleProfile = new UserProfile(
                "user-123", "John", "Doe", "john@example.com",
                "+1234567890", "123 Main St", "1990-01-01",
                "Engineer", 1000.0, "ACCOUNT_HOLDER", "000000000001"
        );
        // Set updatedAt via reflection since there's no setter and @UpdateTimestamp won't fire in unit tests
        Field updatedAtField = UserProfile.class.getDeclaredField("updatedAt");
        updatedAtField.setAccessible(true);
        updatedAtField.set(sampleProfile, LocalDateTime.now());
    }

    @Nested
    @DisplayName("GET /api/user/profile/me")
    class GetMyProfile {

        @Test
        @DisplayName("should return own profile")
        void shouldReturnOwnProfile() throws Exception {
            when(authorizationService.getCurrentUserId()).thenReturn("user-123");
            when(userService.getUserProfile("user-123")).thenReturn(Optional.of(sampleProfile));

            mockMvc.perform(get("/api/user/profile/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value("user-123"))
                    .andExpect(jsonPath("$.firstName").value("John"));
        }

        @Test
        @DisplayName("should return 400 when no user context")
        void shouldReturn400WhenNoUserContext() throws Exception {
            when(authorizationService.getCurrentUserId()).thenReturn(null);

            mockMvc.perform(get("/api/user/profile/me"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("No user context"));
        }

        @Test
        @DisplayName("should return 404 when profile not found")
        void shouldReturn404WhenProfileNotFound() throws Exception {
            when(authorizationService.getCurrentUserId()).thenReturn("user-123");
            when(userService.getUserProfile("user-123")).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/user/profile/me"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/user/profile/me")
    class UpdateMyProfile {

        @Test
        @DisplayName("should update own profile successfully")
        void shouldUpdateOwnProfile() throws Exception {
            when(authorizationService.getCurrentUserId()).thenReturn("user-123");
            when(userService.updateUserProfileFromRequest(eq("user-123"), any(UpdateUserRequest.class)))
                    .thenReturn(sampleProfile);

            UpdateUserRequest request = new UpdateUserRequest("Jane", "Smith", null, null);

            mockMvc.perform(put("/api/user/profile/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Profile updated successfully"))
                    .andExpect(jsonPath("$.userId").value("user-123"));
        }

        @Test
        @DisplayName("should return 400 when no user context")
        void shouldReturn400WhenNoUserContext() throws Exception {
            when(authorizationService.getCurrentUserId()).thenReturn(null);

            mockMvc.perform(put("/api/user/profile/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateUserRequest())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("No user context"));
        }

        @Test
        @DisplayName("should return 400 when update fails")
        void shouldReturn400WhenUpdateFails() throws Exception {
            when(authorizationService.getCurrentUserId()).thenReturn("user-123");
            when(userService.updateUserProfileFromRequest(eq("user-123"), any(UpdateUserRequest.class)))
                    .thenThrow(new RuntimeException("User profile not found for userId: user-123"));

            mockMvc.perform(put("/api/user/profile/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateUserRequest())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Profile update failed"));
        }
    }

    @Nested
    @DisplayName("GET /api/user/secured/profile/{userId}")
    class GetUserProfile {

        @Test
        @DisplayName("should return user profile by id")
        void shouldReturnUserProfileById() throws Exception {
            when(authorizationService.getCurrentUserId()).thenReturn("admin-1");
            when(userService.getUserProfile("user-123")).thenReturn(Optional.of(sampleProfile));

            mockMvc.perform(get("/api/user/secured/profile/user-123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value("user-123"));
        }

        @Test
        @DisplayName("should return 404 when profile not found")
        void shouldReturn404WhenProfileNotFound() throws Exception {
            when(authorizationService.getCurrentUserId()).thenReturn("admin-1");
            when(userService.getUserProfile("non-existent")).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/user/secured/profile/non-existent"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("Profile not found"));
        }
    }

    @Nested
    @DisplayName("PUT /api/user/secured/profile/{userId}")
    class UpdateUserProfile {

        @Test
        @DisplayName("should update user profile successfully")
        void shouldUpdateUserProfile() throws Exception {
            when(authorizationService.getCurrentUserId()).thenReturn("admin-1");
            when(userService.updateUserProfileFromRequest(eq("user-123"), any(UpdateUserRequest.class)))
                    .thenReturn(sampleProfile);

            UpdateUserRequest request = new UpdateUserRequest("Jane", null, null, null);

            mockMvc.perform(put("/api/user/secured/profile/user-123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Profile updated successfully"))
                    .andExpect(jsonPath("$.userId").value("user-123"));
        }

        @Test
        @DisplayName("should return 400 when update fails")
        void shouldReturn400WhenUpdateFails() throws Exception {
            when(authorizationService.getCurrentUserId()).thenReturn("admin-1");
            when(userService.updateUserProfileFromRequest(eq("user-123"), any(UpdateUserRequest.class)))
                    .thenThrow(new RuntimeException("Profile not found"));

            mockMvc.perform(put("/api/user/secured/profile/user-123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new UpdateUserRequest())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Profile update failed"));
        }
    }

    @Nested
    @DisplayName("GET /api/user/search")
    class SearchUsers {

        @Test
        @DisplayName("should return search results")
        void shouldReturnSearchResults() throws Exception {
            when(authorizationService.getCurrentUserId()).thenReturn("user-123");
            when(userService.searchUsers("John")).thenReturn(List.of(sampleProfile));

            mockMvc.perform(get("/api/user/search").param("query", "John"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.query").value("John"))
                    .andExpect(jsonPath("$.totalResults").value(1));
        }

        @Test
        @DisplayName("should return 400 for empty query")
        void shouldReturn400ForEmptyQuery() throws Exception {
            when(authorizationService.getCurrentUserId()).thenReturn("user-123");

            mockMvc.perform(get("/api/user/search").param("query", ""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Invalid search query"));
        }

        @Test
        @DisplayName("should return 500 when search throws exception")
        void shouldReturn500WhenSearchThrows() throws Exception {
            when(authorizationService.getCurrentUserId()).thenReturn("user-123");
            when(userService.searchUsers("error")).thenThrow(new RuntimeException("DB error"));

            mockMvc.perform(get("/api/user/search").param("query", "error"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error").value("Search failed"));
        }
    }

    @Nested
    @DisplayName("PUT /api/user/role/{userId}")
    class UpdateUserRole {

        @Test
        @DisplayName("should update user role successfully")
        void shouldUpdateUserRole() throws Exception {
            when(authorizationService.getCurrentUserId()).thenReturn("admin-1");
            when(userService.changeUserRole("user-123", "ADMIN", "admin-1")).thenReturn(sampleProfile);

            mockMvc.perform(put("/api/user/role/user-123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("role", "ADMIN"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Role updated successfully"))
                    .andExpect(jsonPath("$.newRole").value("ADMIN"));
        }

        @Test
        @DisplayName("should return 400 for empty role")
        void shouldReturn400ForEmptyRole() throws Exception {
            when(authorizationService.getCurrentUserId()).thenReturn("admin-1");

            mockMvc.perform(put("/api/user/role/user-123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("role", ""))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Invalid role"));
        }

        @Test
        @DisplayName("should return 400 when role change fails")
        void shouldReturn400WhenRoleChangeFails() throws Exception {
            when(authorizationService.getCurrentUserId()).thenReturn("admin-1");
            when(userService.changeUserRole("user-123", "ADMIN", "admin-1"))
                    .thenThrow(new RuntimeException("Failed to update"));

            mockMvc.perform(put("/api/user/role/user-123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("role", "ADMIN"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Role update failed"));
        }
    }

    @Nested
    @DisplayName("GET /api/user/health")
    class HealthCheck {

        @Test
        @DisplayName("should return healthy status")
        void shouldReturnHealthyStatus() throws Exception {
            mockMvc.perform(get("/api/user/health"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("healthy"))
                    .andExpect(jsonPath("$.service").value("user-service"));
        }
    }
}
