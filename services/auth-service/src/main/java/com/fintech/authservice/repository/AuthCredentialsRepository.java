package com.fintech.authservice.repository;

import com.fintech.authservice.entity.AuthCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for AuthCredentials entity
 * Handles password and credential operations
 */
@Repository
public interface AuthCredentialsRepository extends JpaRepository<AuthCredentials, Long> {
    
    /**
     * Find credentials by auth core ID
     * Primary method for password verification
     */
    Optional<AuthCredentials> findByAuthCoreId(Long authCoreId);
    
    /**
     * Find credentials by auth core ID and version
     * For password history and version tracking
     */
    Optional<AuthCredentials> findByAuthCoreIdAndVersion(Long authCoreId, Integer version);
    
    /**
     * Check if credentials exist for auth core
     */
    boolean existsByAuthCoreId(Long authCoreId);
    
    /**
     * Find users with weak passwords
     * For security auditing and forced password updates
     */
    @Query("SELECT ac FROM AuthCredentials ac WHERE ac.passwordStrength < :threshold")
    List<AuthCredentials> findByPasswordStrengthLessThan(@Param("threshold") Integer threshold);
    
    /**
     * Find users who must change password
     */
    List<AuthCredentials> findByMustChangePasswordTrue();
    
    /**
     * Find users with expired passwords
     * @param expiryDate - passwords changed before this date are considered expired
     */
    @Query("SELECT ac FROM AuthCredentials ac WHERE ac.passwordChangedAt < :expiryDate OR ac.passwordChangedAt IS NULL")
    List<AuthCredentials> findExpiredPasswords(@Param("expiryDate") LocalDateTime expiryDate);
    
    /**
     * Get latest version number for auth core
     */
    @Query("SELECT MAX(ac.version) FROM AuthCredentials ac WHERE ac.authCoreId = :authCoreId")
    Integer getLatestVersionByAuthCoreId(@Param("authCoreId") Long authCoreId);
    
    /**
     * Count users by password strength range
     */
    @Query("SELECT COUNT(ac) FROM AuthCredentials ac WHERE ac.passwordStrength BETWEEN :minStrength AND :maxStrength")
    long countByPasswordStrengthBetween(@Param("minStrength") Integer minStrength, @Param("maxStrength") Integer maxStrength);
    
    /**
     * Find recently changed passwords
     */
    @Query("SELECT ac FROM AuthCredentials ac WHERE ac.passwordChangedAt >= :since")
    List<AuthCredentials> findRecentPasswordChanges(@Param("since") LocalDateTime since);
}
