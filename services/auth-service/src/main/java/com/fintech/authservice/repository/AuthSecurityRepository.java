package com.fintech.authservice.repository;

import com.fintech.authservice.entity.AuthSecurity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for AuthSecurity entity
 * Handles security tracking and risk management
 */
@Repository
public interface AuthSecurityRepository extends JpaRepository<AuthSecurity, Long> {
    
    /**
     * Find security data by user ID
     * Primary method for security checks during authentication
     */
    Optional<AuthSecurity> findByUserId(String userId);
    
    /**
     * Find users with high risk scores
     * For security monitoring and alerts
     */
    @Query("SELECT asec FROM AuthSecurity asec WHERE asec.riskScore >= :threshold")
    List<AuthSecurity> findHighRiskUsers(@Param("threshold") Integer threshold);
    
    /**
     * Find currently locked users
     */
    @Query("SELECT asec FROM AuthSecurity asec WHERE asec.lockedUntil IS NOT NULL AND asec.lockedUntil > CURRENT_TIMESTAMP")
    List<AuthSecurity> findCurrentlyLockedUsers();
    
    /**
     * Find users with multiple failed attempts
     */
    @Query("SELECT asec FROM AuthSecurity asec WHERE asec.failedLoginAttempts >= :threshold")
    List<AuthSecurity> findUsersWithFailedAttempts(@Param("threshold") Integer threshold);
    
    /**
     * Find users with suspicious activity
     */
    @Query("SELECT asec FROM AuthSecurity asec WHERE asec.suspiciousActivityCount >= :threshold")
    List<AuthSecurity> findUsersWithSuspiciousActivity(@Param("threshold") Integer threshold);
    
    /**
     * Find users who haven't logged in recently
     * For inactive user detection
     */
    @Query("SELECT asec FROM AuthSecurity asec WHERE asec.lastLoginAt < :since OR asec.lastLoginAt IS NULL")
    List<AuthSecurity> findInactiveUsers(@Param("since") LocalDateTime since);
    
    /**
     * Find users logging in from specific IP
     * For forensic analysis
     */
    List<AuthSecurity> findByLastLoginIp(String ipAddress);
    
    /**
     * Find users with specific device fingerprint
     */
    List<AuthSecurity> findByLastDeviceFingerprint(String deviceFingerprint);
    
    /**
     * Reset failed attempts for users whose lock has expired
     */
    @Modifying
    @Query("UPDATE AuthSecurity asec SET asec.failedLoginAttempts = 0, asec.lockedUntil = NULL WHERE asec.lockedUntil < CURRENT_TIMESTAMP")
    int unlockExpiredAccounts();
    
    /**
     * Count users by risk level
     */
    @Query("SELECT COUNT(asec) FROM AuthSecurity asec WHERE asec.riskScore BETWEEN :minRisk AND :maxRisk")
    long countByRiskScoreBetween(@Param("minRisk") Integer minRisk, @Param("maxRisk") Integer maxRisk);
    
    /**
     * Get security statistics
     */
    @Query("SELECT " +
           "COUNT(asec) as totalUsers, " +
           "COUNT(CASE WHEN asec.riskScore >= 70 THEN 1 END) as highRiskUsers, " +
           "COUNT(CASE WHEN asec.lockedUntil > CURRENT_TIMESTAMP THEN 1 END) as lockedUsers, " +
           "AVG(asec.riskScore) as averageRiskScore " +
           "FROM AuthSecurity asec")
    Object[] getSecurityStatistics();
    
    /**
     * Find recent logins for monitoring
     */
    @Query("SELECT asec FROM AuthSecurity asec WHERE asec.lastLoginAt >= :since ORDER BY asec.lastLoginAt DESC")
    List<AuthSecurity> findRecentLogins(@Param("since") LocalDateTime since);
}
