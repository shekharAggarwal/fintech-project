package com.fintech.authorizationservice.repository;

import com.fintech.authorizationservice.entity.FieldAccess;
import com.fintech.authorizationservice.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FieldAccessRepository extends JpaRepository<FieldAccess, Long> {

    @Query("SELECT fa FROM FieldAccess fa JOIN FETCH fa.role WHERE fa.role IN :roles")
    List<FieldAccess> findByRoleIn(@Param("roles") List<Role> roles);


    @Query("SELECT fa.resourceType, fa.allowedFields, fa.config FROM FieldAccess fa  WHERE fa.role = :roleId")
    List<Object[]> findFieldAccessByRoleId(Long roleId);

    /**
     * Find all field access entries for a given role
     */
    @Query("SELECT fa FROM FieldAccess fa WHERE fa.role = :roleId")
    List<FieldAccess> findAllByRole(@Param("roleId") Long roleId);

    /**
     * Check if a field access entry exists for a given role and resource type
     */
    @Query("SELECT COUNT(fa) > 0 FROM FieldAccess fa WHERE fa.role = :roleId AND fa.resourceType = :resourceType")
    boolean existsByRoleAndResourceType(@Param("roleId") Long roleId, @Param("resourceType") String resourceType);
}
