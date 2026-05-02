package com.fintech.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.authservice.dto.request.LoginRequest;
import com.fintech.authservice.dto.request.LogoutRequest;
import com.fintech.authservice.dto.request.RefreshTokenRequest;
import com.fintech.authservice.dto.request.RegistrationRequest;
import com.fintech.authservice.dto.response.AuthenticationResult;
import com.fintech.authservice.dto.response.RefreshTokenResponse;
import com.fintech.authservice.dto.response.RegistrationResult;
import com.fintech.authservice.entity.AuthCore;
import com.fintech.authservice.filter.JwtAuthenticationFilter;
import com.fintech.authservice.service.AuthService;
import com.fintech.authservice.service.RateLimitingService;
import com.fintech.authservice.service.RefreshTokenService;
import com.fintech.authservice.service.SessionService;
import com.fintech.authservice.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @MockBean
    private SessionService sessionService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitingService rateLimitingService;

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginTests {

        @Test
        @DisplayName("returns 200 with tokens on successful authentication")
        void login_success() throws Exception {
            AuthCore authCore = new AuthCore("user-123", "test@example.com");
            AuthenticationResult result = AuthenticationResult.success(authCore, "session-abc", "refresh-token-xyz");

            when(authService.authenticate(eq("test@example.com"), eq("password123"), anyString(), anyString()))
                    .thenReturn(result);
            when(jwtUtil.generateAccessToken("test@example.com", "session-abc"))
                    .thenReturn("access-token-generated");

            LoginRequest request = new LoginRequest("test@example.com", "password123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.userId").value("user-123"))
                    .andExpect(jsonPath("$.email").value("test@example.com"))
                    .andExpect(jsonPath("$.accessToken").value("access-token-generated"))
                    .andExpect(jsonPath("$.refreshToken").value("refresh-token-xyz"));
        }

        @Test
        @DisplayName("returns 200 with failure message on invalid credentials")
        void login_invalidCredentials() throws Exception {
            AuthenticationResult result = AuthenticationResult.failed("Invalid credentials", "INVALID_CREDENTIALS");

            when(authService.authenticate(eq("test@example.com"), eq("wrongpass"), anyString(), anyString()))
                    .thenReturn(result);

            LoginRequest request = new LoginRequest("test@example.com", "wrongpass");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Invalid credentials"))
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                    .andExpect(jsonPath("$.accessToken").doesNotExist());
        }

        @Test
        @DisplayName("returns 400 when email is blank")
        void login_blankEmail() throws Exception {
            String body = "{\"email\":\"\",\"password\":\"password123\"}";

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when password is blank")
        void login_blankPassword() throws Exception {
            String body = "{\"email\":\"test@example.com\",\"password\":\"\"}";

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when email format is invalid")
        void login_invalidEmailFormat() throws Exception {
            String body = "{\"email\":\"not-an-email\",\"password\":\"password123\"}";

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 200 with account locked message")
        void login_accountLocked() throws Exception {
            AuthenticationResult result = AuthenticationResult.failed("Account is locked", "ACCOUNT_LOCKED");

            when(authService.authenticate(eq("locked@example.com"), eq("password123"), anyString(), anyString()))
                    .thenReturn(result);

            LoginRequest request = new LoginRequest("locked@example.com", "password123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Account is locked"))
                    .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/register")
    class RegisterTests {

        @Test
        @DisplayName("returns 200 on successful registration")
        void register_success() throws Exception {
            AuthCore authCore = new AuthCore("new-user-456", "newuser@example.com");
            RegistrationResult result = RegistrationResult.success(authCore, "Registration successful");

            when(authService.registerUser(any(RegistrationRequest.class))).thenReturn(result);

            RegistrationRequest request = new RegistrationRequest(
                    "John", "Doe", "newuser@example.com", "SecureP@ss1",
                    "+1234567890", "123 Main St", "1990-01-15", "Engineer", 1000.0
            );

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.userId").value("new-user-456"))
                    .andExpect(jsonPath("$.message").value("Registration successful"));
        }

        @Test
        @DisplayName("returns 200 with failure when email already exists")
        void register_duplicateEmail() throws Exception {
            RegistrationResult result = RegistrationResult.failed("Email already registered", "EMAIL_EXISTS");

            when(authService.registerUser(any(RegistrationRequest.class))).thenReturn(result);

            RegistrationRequest request = new RegistrationRequest(
                    "John", "Doe", "existing@example.com", "SecureP@ss1",
                    "+1234567890", "123 Main St", "1990-01-15", "Engineer", 1000.0
            );

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Email already registered"))
                    .andExpect(jsonPath("$.code").value("EMAIL_EXISTS"));
        }

        @Test
        @DisplayName("returns 400 when required fields are missing")
        void register_missingFields() throws Exception {
            String body = "{\"email\":\"test@example.com\",\"password\":\"SecureP@ss1\"}";

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when email format is invalid")
        void register_invalidEmail() throws Exception {
            RegistrationRequest request = new RegistrationRequest(
                    "John", "Doe", "invalid-email", "SecureP@ss1",
                    "+1234567890", "123 Main St", "1990-01-15", "Engineer", 1000.0
            );

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when password is too short")
        void register_shortPassword() throws Exception {
            RegistrationRequest request = new RegistrationRequest(
                    "John", "Doe", "test@example.com", "short",
                    "+1234567890", "123 Main St", "1990-01-15", "Engineer", 1000.0
            );

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/refresh")
    class RefreshTokenTests {

        @Test
        @DisplayName("returns 200 with new tokens on valid refresh token")
        void refresh_success() throws Exception {
            RefreshTokenResponse response = RefreshTokenResponse.success("new-access-token", "new-refresh-token");

            when(refreshTokenService.refreshAccessToken("valid-refresh-token")).thenReturn(response);

            RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");

            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                    .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
        }

        @Test
        @DisplayName("returns 401 when refresh token is invalid")
        void refresh_invalidToken() throws Exception {
            RefreshTokenResponse response = RefreshTokenResponse.failed("Invalid refresh token", "INVALID_TOKEN");

            when(refreshTokenService.refreshAccessToken("invalid-token")).thenReturn(response);

            RefreshTokenRequest request = new RefreshTokenRequest("invalid-token");

            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Invalid refresh token"))
                    .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
        }

        @Test
        @DisplayName("returns 401 when refresh token is expired")
        void refresh_expiredToken() throws Exception {
            RefreshTokenResponse response = RefreshTokenResponse.failed("Refresh token expired", "TOKEN_EXPIRED");

            when(refreshTokenService.refreshAccessToken("expired-token")).thenReturn(response);

            RefreshTokenRequest request = new RefreshTokenRequest("expired-token");

            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
        }

        @Test
        @DisplayName("returns 400 when refresh token is blank")
        void refresh_blankToken() throws Exception {
            String body = "{\"refreshToken\":\"\"}";

            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout")
    class LogoutTests {

        @Test
        @DisplayName("returns 200 on successful logout")
        void logout_success() throws Exception {
            doNothing().when(refreshTokenService).logout("refresh-token-abc", "access-token-xyz");

            LogoutRequest request = new LogoutRequest("refresh-token-abc");

            mockMvc.perform(post("/api/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer access-token-xyz")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Logged out successfully"));

            verify(refreshTokenService).logout("refresh-token-abc", "access-token-xyz");
        }

        @Test
        @DisplayName("returns 200 on logout without Authorization header")
        void logout_noAuthHeader() throws Exception {
            doNothing().when(refreshTokenService).logout("refresh-token-abc", null);

            LogoutRequest request = new LogoutRequest("refresh-token-abc");

            mockMvc.perform(post("/api/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Logged out successfully"));

            verify(refreshTokenService).logout("refresh-token-abc", null);
        }

        @Test
        @DisplayName("returns 400 when refresh token is blank")
        void logout_blankRefreshToken() throws Exception {
            String body = "{\"refreshToken\":\"\"}";

            mockMvc.perform(post("/api/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer some-token")
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout-all")
    class LogoutAllTests {

        @Test
        @DisplayName("returns 200 on successful logout from all devices")
        void logoutAll_success() throws Exception {
            when(jwtUtil.validateToken("valid-access-token")).thenReturn(true);
            when(jwtUtil.getSessionIdFromToken("valid-access-token")).thenReturn("session-123");
            when(sessionService.getUserIdFromSession("session-123")).thenReturn("user-456");
            doNothing().when(refreshTokenService).logoutAllDevices("user-456");
            doNothing().when(refreshTokenService).logout(null, "valid-access-token");

            mockMvc.perform(post("/api/auth/logout-all")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer valid-access-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Logged out from all devices successfully"));

            verify(refreshTokenService).logoutAllDevices("user-456");
            verify(refreshTokenService).logout(null, "valid-access-token");
        }

        @Test
        @DisplayName("returns 401 when no Authorization header is present")
        void logoutAll_noAuthHeader() throws Exception {
            mockMvc.perform(post("/api/auth/logout-all")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Valid authentication required"));
        }

        @Test
        @DisplayName("returns 401 when token is invalid")
        void logoutAll_invalidToken() throws Exception {
            when(jwtUtil.validateToken("invalid-token")).thenReturn(false);

            mockMvc.perform(post("/api/auth/logout-all")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer invalid-token"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Valid authentication required"));
        }

        @Test
        @DisplayName("returns 401 when session is not found")
        void logoutAll_sessionNotFound() throws Exception {
            when(jwtUtil.validateToken("valid-token")).thenReturn(true);
            when(jwtUtil.getSessionIdFromToken("valid-token")).thenReturn("expired-session");
            when(sessionService.getUserIdFromSession("expired-session")).thenReturn(null);

            mockMvc.perform(post("/api/auth/logout-all")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer valid-token"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Session not found"));
        }
    }
}
