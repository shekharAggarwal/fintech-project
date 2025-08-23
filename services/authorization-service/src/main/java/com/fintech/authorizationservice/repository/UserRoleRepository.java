package com.fintech.authorizationservice.repository;

import com.fintech.authorizationservice.entity.Role;
import com.fintech.authorizationservice.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    
    /**
     * Find all roles for a specific user
     */
    @Query("SELECT ur FROM UserRole ur JOIN FETCH ur.role WHERE ur.userId = :userId")
    List<UserRole> findByUserId(@Param("userId") String userId);
    
    /**
     * Find a specific user-role combination
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.userId = :userId AND ur.role = :role")
    Optional<UserRole> findByUserIdAndRole(@Param("userId") String userId, @Param("role") Role role);
    
    /**
     * Check if user has a specific role
     */
    @Query("SELECT COUNT(ur) > 0 FROM UserRole ur WHERE ur.userId = :userId AND ur.role.name = :roleName")
    boolean existsByUserIdAndRoleName(@Param("userId") String userId, @Param("roleName") String roleName);
    
    /**
     * Get all role names for a user
     */
    @Query("SELECT ur.role.name FROM UserRole ur WHERE ur.userId = :userId")
    List<String> findRoleNamesByUserId(@Param("userId") String userId);
}
