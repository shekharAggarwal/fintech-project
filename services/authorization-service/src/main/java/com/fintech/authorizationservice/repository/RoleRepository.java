package com.fintech.authorizationservice.repository;

import com.fintech.authorizationservice.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    
    Optional<Role> findByNameAndIsActiveTrue(String name);
    
    @Query("SELECT r FROM Role r JOIN FETCH r.permissions rp JOIN FETCH rp.permission p " +
           "WHERE r.name = :roleName AND r.isActive = true AND p.isActive = true")
    Optional<Role> findByNameWithPermissions(String roleName);
}
