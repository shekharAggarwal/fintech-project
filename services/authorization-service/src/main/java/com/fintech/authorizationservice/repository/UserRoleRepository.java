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
     * Find a specific user-role combination
     */
    @Query("SELECT ur FROM UserRole ur WHERE ur.userId = :userId AND ur.role = :role")
    Optional<UserRole> findByUserIdAndRole(@Param("userId") String userId, @Param("role") Role role);

    /**
     * Find role ID by user ID
     */
    @Query("SELECT ur.role FROM UserRole ur WHERE ur.userId = :userId")
    Optional<Long> findRoleIdByUserId(@Param("userId") String userId);

    /**
     * Check if user has a specific role by role name
     */
    @Query("SELECT COUNT(ur) > 0 FROM UserRole ur JOIN Role r ON ur.role = r.roleId WHERE ur.userId = :userId AND r.name = :roleName")
    boolean existsByUserIdAndRoleName(@Param("userId") String userId, @Param("roleName") String roleName);

    /**
     * Find role names by user ID
     */
    @Query("SELECT r.name FROM UserRole ur JOIN Role r ON ur.role = r.roleId WHERE ur.userId = :userId")
    List<String> findRoleNamesByUserId(@Param("userId") String userId);
}
