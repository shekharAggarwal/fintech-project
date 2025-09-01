package com.fintech.authorizationservice.repository;

import com.fintech.authorizationservice.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
    
    @Query("SELECT r FROM Role r WHERE r.name IN :roleNames")
    List<Role> findByNameIn(@Param("roleNames") List<String> roleNames);
}