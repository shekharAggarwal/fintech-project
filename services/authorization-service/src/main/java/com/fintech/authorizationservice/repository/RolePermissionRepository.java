package com.fintech.authorizationservice.repository;

import com.fintech.authorizationservice.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    @Query("SELECT rp from RolePermission rp where rp.role=:roleId and rp.apiMethodId=:methodId and rp.allowed=true")
    List<RolePermission> findMatchingPermissions(Long roleId, Long methodId);

    /**
     * Check if a permission exists for a given role and API method
     */
    @Query("SELECT COUNT(rp) > 0 FROM RolePermission rp WHERE rp.role = :roleId AND rp.apiMethodId = :apiMethodId")
    boolean existsByRoleAndApiMethodId(@Param("roleId") Long roleId, @Param("apiMethodId") Long apiMethodId);

    /**
     * Delete all permissions for a given role
     */
    @Modifying
    @Query("DELETE FROM RolePermission rp WHERE rp.role = :roleId")
    void deleteByRole(@Param("roleId") Long roleId);
}
