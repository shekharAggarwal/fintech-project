package com.fintech.authservice.repository;

import com.fintech.authservice.entity.AuthCore;
import com.fintech.authservice.entity.AuthCore.AuthStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for optimized AuthCore entity
 * Fast lookups for authentication operations
 */
@Repository
public interface AuthCoreRepository extends JpaRepository<AuthCore, Long> {
    
    /**
     * Find user by email - primary login method
     * Uses unique index for fast lookup
     */
    Optional<AuthCore> findByEmail(String email);


    /**
     * Find user by userId - for cross-service communication
     * Uses unique index for fast lookup
     */
    Optional<AuthCore> findByUserId(String userId);
    
    /**
     * Find user by email and status - optimized login query
     * Uses composite index for optimal performance
     */
    Optional<AuthCore> findByEmailAndStatus(String email, AuthStatus status);
    
    /**
     * Find all active verified users
     * Uses composite index for filtering
     */
    @Query("SELECT ac FROM AuthCore ac WHERE ac.status = :status AND ac.emailVerified = true AND ac.deletedAt IS NULL")
    List<AuthCore> findActiveVerifiedUsers(@Param("status") AuthStatus status);
    
    /**
     * Find users by shard key for horizontal scaling
     */
    // Removed invalid method: findByShardKey(Integer shardKey)
    
    /**
     * Check if email exists (for registration validation)
     * Faster than findByEmail when only existence check is needed
     */
    boolean existsByEmail(String email);
    
    /**
     * Check if userId exists
     */
    boolean existsByUserId(String userId);
    
    /**
     * Find users created within date range
     */
    @Query("SELECT ac FROM AuthCore ac WHERE ac.createdAt BETWEEN :startDate AND :endDate AND ac.deletedAt IS NULL")
    List<AuthCore> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * Count active users by status
     */
    long countByStatusAndDeletedAtIsNull(AuthStatus status);

    /**
     * Find users needing email verification
     */
    @Query("SELECT ac FROM AuthCore ac WHERE ac.emailVerified = false AND ac.status = 'PENDING_VERIFICATION' AND ac.deletedAt IS NULL")
    List<AuthCore> findUsersNeedingEmailVerification();

    /**
     * Soft delete user by userId
     */
    @Query("UPDATE AuthCore ac SET ac.status = 'DELETED', ac.deletedAt = CURRENT_TIMESTAMP WHERE ac.userId = :userId")
    int softDeleteByUserId(@Param("userId") String userId);
}
