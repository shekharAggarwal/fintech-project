package com.fintech.authorizationservice.controller.admin;

import com.fintech.authorizationservice.dto.request.admin.CreatePermissionRequest;
import com.fintech.authorizationservice.dto.request.admin.UpdatePermissionRequest;
import com.fintech.authorizationservice.dto.response.admin.PermissionResponse;
import com.fintech.authorizationservice.service.AdminService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/permissions")
public class PermissionController {

    private static final Logger log = LoggerFactory.getLogger(PermissionController.class);
    private final AdminService adminService;

    public PermissionController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping
    public ResponseEntity<PermissionResponse> createPermission(@Valid @RequestBody CreatePermissionRequest request) {
        log.info("POST /api/admin/permissions - Creating permission for roleId={}, apiMethodId={}", request.getRoleId(), request.getApiMethodId());
        PermissionResponse response = adminService.createPermission(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<PermissionResponse>> getAllPermissions() {
        log.info("GET /api/admin/permissions - Fetching all permissions");
        List<PermissionResponse> permissions = adminService.getAllPermissions();
        return ResponseEntity.ok(permissions);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PermissionResponse> updatePermission(@PathVariable Long id, @RequestBody UpdatePermissionRequest request) {
        log.info("PUT /api/admin/permissions/{} - Updating permission", id);
        PermissionResponse response = adminService.updatePermission(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deletePermission(@PathVariable Long id) {
        log.info("DELETE /api/admin/permissions/{} - Deleting permission", id);
        adminService.deletePermission(id);
        return ResponseEntity.ok(Map.of("message", "Permission deleted successfully", "permissionId", id.toString()));
    }
}
