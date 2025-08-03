package com.fintech.authservice.repository;

import com.fintech.authservice.entity.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuthUserRepository extends JpaRepository<AuthUser, Long> {
    
    Optional<AuthUser> findByEmail(String email);
    
    Optional<AuthUser> findByUserId(String userId);
    
    @Query("SELECT u FROM AuthUser u WHERE u.email = :email AND u.status = 'ACTIVE' AND u.deletedAt IS NULL")
    Optional<AuthUser> findActiveUserByEmail(@Param("email") String email);
    
    @Query("SELECT u FROM AuthUser u WHERE u.userId = :userId AND u.status = 'ACTIVE' AND u.deletedAt IS NULL")
    Optional<AuthUser> findActiveUserByUserId(@Param("userId") String userId);
    
    @Query("SELECT u FROM AuthUser u WHERE u.currentSessionId = :sessionId AND u.status = 'ACTIVE' AND u.deletedAt IS NULL")
    Optional<AuthUser> findByCurrentSessionId(@Param("sessionId") String sessionId);
    
    boolean existsByEmail(String email);
    
    boolean existsByUserId(String userId);
    
    @Query("SELECT u FROM AuthUser u WHERE u.status = 'LOCKED' AND u.lockedUntil < :now")
    List<AuthUser> findExpiredLockedUsers(@Param("now") LocalDateTime now);
    
    @Query("SELECT u FROM AuthUser u WHERE u.emailVerified = false AND u.emailVerificationTokenExpiresAt < :now")
    List<AuthUser> findExpiredUnverifiedUsers(@Param("now") LocalDateTime now);
    
    @Query("SELECT u FROM AuthUser u WHERE u.refreshTokenExpiresAt < :now AND u.refreshTokenHash IS NOT NULL")
    List<AuthUser> findUsersWithExpiredRefreshTokens(@Param("now") LocalDateTime now);
    
    @Query("SELECT u FROM AuthUser u WHERE u.lastLoginAt < :cutoffDate AND u.status = 'ACTIVE'")
    List<AuthUser> findInactiveUsers(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("SELECT COUNT(u) FROM AuthUser u WHERE u.shardKey = :shardKey")
    long countByShardKey(@Param("shardKey") Integer shardKey);
    
    @Query("SELECT u FROM AuthUser u WHERE u.failedLoginAttempts >= :threshold AND u.status != 'LOCKED'")
    List<AuthUser> findUsersWithHighFailedAttempts(@Param("threshold") Integer threshold);
    
    @Query("SELECT COUNT(u) FROM AuthUser u WHERE u.status = :status")
    long countByStatus(@Param("status") AuthUser.AuthStatus status);
    
    @Query("SELECT u FROM AuthUser u WHERE u.passwordChangedAt < :cutoffDate AND u.passwordMustChange = false")
    List<AuthUser> findUsersWithOldPasswords(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("SELECT u FROM AuthUser u WHERE u.emailVerificationToken = :token")
    Optional<AuthUser> findByEmailVerificationToken(@Param("token") String token);
}
