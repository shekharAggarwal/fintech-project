package com.fintech.authorizationservice.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.authorizationservice.dto.request.admin.CreateRoleRequest;
import com.fintech.authorizationservice.dto.request.admin.UpdateRoleRequest;
import com.fintech.authorizationservice.dto.response.admin.RoleResponse;
import com.fintech.authorizationservice.exception.DuplicateResourceException;
import com.fintech.authorizationservice.exception.ResourceInUseException;
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

@WebMvcTest(RoleController.class)
@AutoConfigureMockMvc(addFilters = false)
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminService adminService;

    @Test
    void createRole_success_returns201() throws Exception {
        RoleResponse response = new RoleResponse(1L, "ADMIN", "Administrator");
        when(adminService.createRole(any(CreateRoleRequest.class))).thenReturn(response);

        String body = objectMapper.writeValueAsString(new CreateRoleRequest("ADMIN", "Administrator"));

        mockMvc.perform(post("/api/admin/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roleId").value(1))
                .andExpect(jsonPath("$.name").value("ADMIN"))
                .andExpect(jsonPath("$.description").value("Administrator"));
    }

    @Test
    void createRole_duplicate_returns409() throws Exception {
        when(adminService.createRole(any(CreateRoleRequest.class)))
                .thenThrow(new DuplicateResourceException("Role", "name", "ADMIN"));

        String body = objectMapper.writeValueAsString(new CreateRoleRequest("ADMIN", "desc"));

        mockMvc.perform(post("/api/admin/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void createRole_invalidBody_returns400() throws Exception {
        // Name is blank (violates @NotBlank)
        String body = objectMapper.writeValueAsString(new CreateRoleRequest("", "desc"));

        mockMvc.perform(post("/api/admin/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllRoles_returnsPage() throws Exception {
        RoleResponse role = new RoleResponse(1L, "ADMIN", "Administrator");
        Page<RoleResponse> page = new PageImpl<>(List.of(role), PageRequest.of(0, 20), 1);
        when(adminService.getAllRoles(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/roles")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("ADMIN"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getAllRoles_defaultPagination() throws Exception {
        Page<RoleResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(adminService.getAllRoles(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getRoleById_found_returns200() throws Exception {
        RoleResponse response = new RoleResponse(1L, "ADMIN", "Administrator");
        when(adminService.getRoleById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/admin/roles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roleId").value(1))
                .andExpect(jsonPath("$.name").value("ADMIN"));
    }

    @Test
    void getRoleById_notFound_returns404() throws Exception {
        when(adminService.getRoleById(99L)).thenThrow(new ResourceNotFoundException("Role", "id", 99L));

        mockMvc.perform(get("/api/admin/roles/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateRole_success_returns200() throws Exception {
        RoleResponse response = new RoleResponse(1L, "SUPER_ADMIN", "Updated");
        when(adminService.updateRole(eq(1L), any(UpdateRoleRequest.class))).thenReturn(response);

        String body = objectMapper.writeValueAsString(new UpdateRoleRequest("SUPER_ADMIN", "Updated"));

        mockMvc.perform(put("/api/admin/roles/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("SUPER_ADMIN"));
    }

    @Test
    void updateRole_notFound_returns404() throws Exception {
        when(adminService.updateRole(eq(99L), any(UpdateRoleRequest.class)))
                .thenThrow(new ResourceNotFoundException("Role", "id", 99L));

        String body = objectMapper.writeValueAsString(new UpdateRoleRequest("NEW", "desc"));

        mockMvc.perform(put("/api/admin/roles/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRole_success_returns204() throws Exception {
        doNothing().when(adminService).deleteRole(1L);

        mockMvc.perform(delete("/api/admin/roles/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteRole_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Role", "id", 99L)).when(adminService).deleteRole(99L);

        mockMvc.perform(delete("/api/admin/roles/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteRole_inUse_returns409() throws Exception {
        doThrow(new ResourceInUseException("Role", 1L, "Role is assigned to users"))
                .when(adminService).deleteRole(1L);

        mockMvc.perform(delete("/api/admin/roles/1"))
                .andExpect(status().isConflict());
    }
}
