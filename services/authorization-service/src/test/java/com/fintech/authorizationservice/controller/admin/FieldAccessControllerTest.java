package com.fintech.authorizationservice.controller.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.authorizationservice.dto.request.admin.CreateFieldAccessRequest;
import com.fintech.authorizationservice.dto.request.admin.UpdateFieldAccessRequest;
import com.fintech.authorizationservice.dto.response.admin.FieldAccessResponse;
import com.fintech.authorizationservice.exception.DuplicateResourceException;
import com.fintech.authorizationservice.exception.ResourceNotFoundException;
import com.fintech.authorizationservice.service.AdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FieldAccessController.class)
@AutoConfigureMockMvc(addFilters = false)
class FieldAccessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminService adminService;

    @Test
    void createFieldAccess_success_returns201() throws Exception {
        FieldAccessResponse response = new FieldAccessResponse(1L, 1L, "USER", "[\"name\",\"email\"]", "{\"mask\":true}");
        when(adminService.createFieldAccess(any(CreateFieldAccessRequest.class))).thenReturn(response);

        String body = objectMapper.writeValueAsString(
                new CreateFieldAccessRequest(1L, "USER", "[\"name\",\"email\"]", "{\"mask\":true}"));

        mockMvc.perform(post("/api/admin/field-access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.roleId").value(1))
                .andExpect(jsonPath("$.resourceType").value("USER"));
    }

    @Test
    void createFieldAccess_duplicate_returns409() throws Exception {
        when(adminService.createFieldAccess(any(CreateFieldAccessRequest.class)))
                .thenThrow(new DuplicateResourceException("FieldAccess", "roleId+resourceType", "1+USER"));

        String body = objectMapper.writeValueAsString(
                new CreateFieldAccessRequest(1L, "USER", null, null));

        mockMvc.perform(post("/api/admin/field-access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void createFieldAccess_roleNotFound_returns404() throws Exception {
        when(adminService.createFieldAccess(any(CreateFieldAccessRequest.class)))
                .thenThrow(new ResourceNotFoundException("Role", "id", 99L));

        String body = objectMapper.writeValueAsString(
                new CreateFieldAccessRequest(99L, "USER", null, null));

        mockMvc.perform(post("/api/admin/field-access")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void getFieldAccessByRole_success_returns200() throws Exception {
        FieldAccessResponse fa = new FieldAccessResponse(1L, 1L, "USER", "[\"name\"]", null);
        when(adminService.getFieldAccessByRole(1L)).thenReturn(List.of(fa));

        mockMvc.perform(get("/api/admin/field-access")
                        .param("role", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].resourceType").value("USER"));
    }

    @Test
    void getFieldAccessByRole_roleNotFound_returns404() throws Exception {
        when(adminService.getFieldAccessByRole(99L))
                .thenThrow(new ResourceNotFoundException("Role", "id", 99L));

        mockMvc.perform(get("/api/admin/field-access")
                        .param("role", "99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getFieldAccessByRole_missingParam_returns400() throws Exception {
        mockMvc.perform(get("/api/admin/field-access"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateFieldAccess_success_returns200() throws Exception {
        FieldAccessResponse response = new FieldAccessResponse(10L, 1L, "USER", "[\"name\",\"email\"]", "{\"mask\":false}");
        when(adminService.updateFieldAccess(eq(10L), any(UpdateFieldAccessRequest.class))).thenReturn(response);

        String body = objectMapper.writeValueAsString(new UpdateFieldAccessRequest("[\"name\",\"email\"]", "{\"mask\":false}"));

        mockMvc.perform(put("/api/admin/field-access/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.allowedFields").value("[\"name\",\"email\"]"));
    }

    @Test
    void updateFieldAccess_notFound_returns404() throws Exception {
        when(adminService.updateFieldAccess(eq(99L), any(UpdateFieldAccessRequest.class)))
                .thenThrow(new ResourceNotFoundException("FieldAccess", "id", 99L));

        String body = objectMapper.writeValueAsString(new UpdateFieldAccessRequest(null, null));

        mockMvc.perform(put("/api/admin/field-access/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteFieldAccess_success_returns204() throws Exception {
        doNothing().when(adminService).deleteFieldAccess(1L);

        mockMvc.perform(delete("/api/admin/field-access/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteFieldAccess_notFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("FieldAccess", "id", 99L)).when(adminService).deleteFieldAccess(99L);

        mockMvc.perform(delete("/api/admin/field-access/99"))
                .andExpect(status().isNotFound());
    }
}
