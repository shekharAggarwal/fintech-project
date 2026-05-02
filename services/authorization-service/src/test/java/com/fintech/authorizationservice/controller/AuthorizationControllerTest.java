package com.fintech.authorizationservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.authorizationservice.dto.request.AuthzIntrospectRequest;
import com.fintech.authorizationservice.dto.request.UpdateUserRoleRequest;
import com.fintech.authorizationservice.dto.response.AuthzIntrospectResponse;
import com.fintech.authorizationservice.service.AuthzService;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthorizationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthorizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthzService authzService;

    @MockitoBean
    private Tracer tracer;

    @Test
    void introspect_allowed_returns200() throws Exception {
        AuthzIntrospectResponse response = new AuthzIntrospectResponse(
                true, "user-1", "ADMIN", List.of("/api/test"), null, null);
        when(authzService.introspect(any(AuthzIntrospectRequest.class))).thenReturn(Mono.just(response));

        String body = objectMapper.writeValueAsString(
                new AuthzIntrospectRequest("token", "/api/test", "GET", null));

        mockMvc.perform(post("/api/authz/introspect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true))
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void introspect_denied_returns200WithDenied() throws Exception {
        AuthzIntrospectResponse response = new AuthzIntrospectResponse(
                false, null, null, null, null, "INVALID_TOKEN");
        when(authzService.introspect(any(AuthzIntrospectRequest.class))).thenReturn(Mono.just(response));

        String body = objectMapper.writeValueAsString(
                new AuthzIntrospectRequest("bad-token", "/api/test", "GET", null));

        mockMvc.perform(post("/api/authz/introspect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(false))
                .andExpect(jsonPath("$.reason").value("INVALID_TOKEN"));
    }

    @Test
    void updateUserRole_success_returns200() throws Exception {
        Span span = mock(Span.class);
        TraceContext traceContext = mock(TraceContext.class);
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn("trace-123");
        when(traceContext.spanId()).thenReturn("span-123");
        doNothing().when(authzService).updateUserRole("user-1", "ADMIN", "system");

        UpdateUserRoleRequest request = new UpdateUserRoleRequest("user-1", "ADMIN", "system", "user-service");
        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(put("/api/authz/internal/user-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User role updated successfully"))
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.newRole").value("ADMIN"));
    }

    @Test
    void updateUserRole_emptyUserId_returnsBadRequest() throws Exception {
        Span span = mock(Span.class);
        TraceContext traceContext = mock(TraceContext.class);
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn("trace-123");
        when(traceContext.spanId()).thenReturn("span-123");

        UpdateUserRoleRequest request = new UpdateUserRoleRequest("", "ADMIN", "system", "user-service");
        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(put("/api/authz/internal/user-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid userId"));
    }

    @Test
    void updateUserRole_emptyRole_returnsBadRequest() throws Exception {
        Span span = mock(Span.class);
        TraceContext traceContext = mock(TraceContext.class);
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn("trace-123");
        when(traceContext.spanId()).thenReturn("span-123");

        UpdateUserRoleRequest request = new UpdateUserRoleRequest("user-1", "", "system", "user-service");
        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(put("/api/authz/internal/user-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid role"));
    }

    @Test
    void updateUserRole_emptyServiceSource_returnsBadRequest() throws Exception {
        Span span = mock(Span.class);
        TraceContext traceContext = mock(TraceContext.class);
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn("trace-123");
        when(traceContext.spanId()).thenReturn("span-123");

        UpdateUserRoleRequest request = new UpdateUserRoleRequest("user-1", "ADMIN", "system", "");
        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(put("/api/authz/internal/user-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid serviceSource"));
    }

    @Test
    void updateUserRole_serviceThrows_returnsBadRequest() throws Exception {
        Span span = mock(Span.class);
        TraceContext traceContext = mock(TraceContext.class);
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn("trace-123");
        when(traceContext.spanId()).thenReturn("span-123");
        doThrow(new RuntimeException("Role not found")).when(authzService).updateUserRole("user-1", "NONEXIST", "system");

        UpdateUserRoleRequest request = new UpdateUserRoleRequest("user-1", "NONEXIST", "system", "user-service");
        String body = objectMapper.writeValueAsString(request);

        mockMvc.perform(put("/api/authz/internal/user-role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Role update failed"));
    }
}
