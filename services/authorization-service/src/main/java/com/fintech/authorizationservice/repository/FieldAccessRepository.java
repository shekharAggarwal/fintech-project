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

    /**
     * Optimized query to get field access using role IDs directly
     */
    @Query(value = """
        SELECT fa.resource_type, fa.allowed_fields
        FROM field_access fa
        WHERE fa.role_id IN :roleIds
        """, nativeQuery = true)
    List<Object[]> findFieldAccessByRoleIds(@Param("roleIds") List<Long> roleIds);
}
