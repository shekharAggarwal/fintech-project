package com.fintech.authorizationservice.controller.admin;

import com.fintech.authorizationservice.dto.request.admin.CreateFieldAccessRequest;
import com.fintech.authorizationservice.dto.request.admin.UpdateFieldAccessRequest;
import com.fintech.authorizationservice.dto.response.admin.FieldAccessResponse;
import com.fintech.authorizationservice.service.AdminService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fintech.security.annotation.RequireAuthorization;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/field-access")
@RequireAuthorization(roles = {"ADMIN"})
public class FieldAccessController {

    private static final Logger log = LoggerFactory.getLogger(FieldAccessController.class);
    private final AdminService adminService;

    public FieldAccessController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping
    public ResponseEntity<FieldAccessResponse> createFieldAccess(@Valid @RequestBody CreateFieldAccessRequest request) {
        log.info("POST /api/admin/field-access - Creating field access for roleId={}, resourceType={}", request.getRoleId(), request.getResourceType());
        FieldAccessResponse response = adminService.createFieldAccess(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<FieldAccessResponse>> getFieldAccessByRole(@RequestParam("role") Long roleId) {
        log.info("GET /api/admin/field-access?role={} - Fetching field access", roleId);
        List<FieldAccessResponse> fieldAccess = adminService.getFieldAccessByRole(roleId);
        return ResponseEntity.ok(fieldAccess);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FieldAccessResponse> updateFieldAccess(@PathVariable Long id, @Valid @RequestBody UpdateFieldAccessRequest request) {
        log.info("PUT /api/admin/field-access/{} - Updating field access", id);
        FieldAccessResponse response = adminService.updateFieldAccess(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFieldAccess(@PathVariable Long id) {
        log.info("DELETE /api/admin/field-access/{} - Deleting field access", id);
        adminService.deleteFieldAccess(id);
        return ResponseEntity.noContent().build();
    }
}
