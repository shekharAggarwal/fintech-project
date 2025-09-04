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
}
