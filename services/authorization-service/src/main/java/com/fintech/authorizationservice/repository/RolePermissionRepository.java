package com.fintech.authorizationservice.repository;

import com.fintech.authorizationservice.entity.Role;
import com.fintech.authorizationservice.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    List<RolePermission> findByRoleIn(List<Role> roles);
}
