package com.fintech.authorizationservice.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.authorizationservice.dto.request.admin.CreatePermissionRequest;
import com.fintech.authorizationservice.dto.request.admin.UpdatePermissionRequest;
import com.fintech.authorizationservice.dto.response.admin.PermissionResponse;
import com.fintech.authorizationservice.exception.DuplicateResourceException;
import com.fintech.authorizationservice.exception.ResourceNotFoundException;
import com.fintech.authorizationservice.service.AdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PermissionController.class)
@AutoConfigureMockMvc(addFilters = false)
class PermissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminService adminService;

    @Test
    void createPermission_success_returns201() throws Exception {
        PermissionResponse response = new PermissionResponse(1L, 1L, 10L, true);
        when(adminService.createPermission(any(CreatePermissionRequest.class))).thenReturn(response);

        String body = objectMapper.writeValueAsString(new CreatePermissionRequest(1L, 10L, true));

        mockMvc.perform(post("/api/admin/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.roleId").value(1))
                .andExpect(jsonPath("$.apiMethodId").value(10))
                .andExpect(jsonPath("$.allowed").value(true));
    }

    @Test
    void createPermission_duplicate_returns409() throws Exception {
        when(adminService.createPermission(any(CreatePermissionRequest.class)))
                .thenThrow(new DuplicateResourceException("Permission", "roleId+apiMethodId", "1+10"));

        String body = objectMapper.writeValueAsString(new CreatePermissionRequest(1L, 10L, true));

        mockMvc.perform(post("/api/admin/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void createPermission_roleNotFound_returns404() throws Exception {
        when(adminService.createPermission(any(CreatePermissionRequest.class)))
                .thenThrow(new ResourceNotFoundException("Role", "id", 99L));

        String body = objectMapper.writeValueAsString(new CreatePermissionRequest(99L, 10L, true));

        mockMvc.perform(post("/api/admin/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllPermissions_returnsPage() throws Exception {
        PermissionResponse perm = new PermissionResponse(1L, 1L, 10L, true);
        Page<PermissionResponse> page = new PageImpl<>(List.of(perm), PageRequest.of(0, 20), 1);
        when(adminService.getAllPermissions(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/permissions")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].allowed").value(true))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getAllPermissions_defaultPagination() throws Exception {
        Page<PermissionResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(adminService.getAllPermissions(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void updatePermission_success_returns200() throws Exception {
        PermissionResponse response = new PermissionResponse(5L, 1L, 10L, false);
        when(adminService.updatePermission(eq(5L), any(UpdatePermissionRequest.class))).thenReturn(response);

        String body = objectMapper.writeValueAsString(new UpdatePermissionRequest(false));

        mockMvc.perform(put("/api/admin/permissions/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.allowed").value(false));
    }

    @Test
    void updatePermission_notFound_returns404() throws Exception {
        when(adminService.updatePermission(eq(99L), any(UpdatePermissionRequest.class)))
                .thenThrow(new ResourceNotFoundException("Permission", "id", 99L));

        String body = objectMapper.writeValueAsString(new UpdatePermissionRequest(true));

        mockMvc.perform(put("/api/admin/permissions/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletePermission_success_returns204() throws Exception {
        doNothing().when(adminService).deletePermission(1L);

        mockMvc.perform(delete("/api/admin/permissions/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deletePermission_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Permission", "id", 99L)).when(adminService).deletePermission(99L);

        mockMvc.perform(delete("/api/admin/permissions/99"))
                .andExpect(status().isNotFound());
    }
}
