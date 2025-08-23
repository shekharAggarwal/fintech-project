package com.fintech.authorizationservice.repository;

import com.fintech.authorizationservice.entity.FieldAccess;
import com.fintech.authorizationservice.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FieldAccessRepository extends JpaRepository<FieldAccess, Long> {
    List<FieldAccess> findByRoleIn(List<Role> roles);
}
