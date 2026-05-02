package com.fintech.authorizationservice.controller.admin;

import com.fintech.authorizationservice.dto.request.admin.CreateRoleRequest;
import com.fintech.authorizationservice.dto.request.admin.UpdateRoleRequest;
import com.fintech.authorizationservice.dto.response.admin.RoleResponse;
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
@RequestMapping("/api/admin/roles")
@RequireAuthorization(roles = {"ADMIN"})
public class RoleController {

    private static final Logger log = LoggerFactory.getLogger(RoleController.class);
    private final AdminService adminService;

    public RoleController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
        log.info("POST /api/admin/roles - Creating role: {}", request.getName());
        RoleResponse response = adminService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        log.info("GET /api/admin/roles - Fetching all roles");
        List<RoleResponse> roles = adminService.getAllRoles();
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoleResponse> getRoleById(@PathVariable Long id) {
        log.info("GET /api/admin/roles/{} - Fetching role", id);
        RoleResponse role = adminService.getRoleById(id);
        return ResponseEntity.ok(role);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RoleResponse> updateRole(@PathVariable Long id, @Valid @RequestBody UpdateRoleRequest request) {
        log.info("PUT /api/admin/roles/{} - Updating role", id);
        RoleResponse response = adminService.updateRole(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        log.info("DELETE /api/admin/roles/{} - Deleting role", id);
        adminService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }
}
