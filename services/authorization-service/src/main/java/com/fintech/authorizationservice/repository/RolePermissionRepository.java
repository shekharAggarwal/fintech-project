package com.fintech.authorizationservice.repository;

import com.fintech.authorizationservice.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    @Query("SELECT rp from RolePermission rp where rp.role=:roleId and rp.apiMethodId=:methodId and rp.allowed=true")
    List<RolePermission> findMatchingPermissions(Long roleId, Long methodId);
}
