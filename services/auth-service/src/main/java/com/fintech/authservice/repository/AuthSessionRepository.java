package com.fintech.authservice.repository;

import com.fintech.authservice.entity.AuthSession;
import com.fintech.authservice.entity.AuthSession.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for AuthSession entity
 * Ultra-fast session management operations
 */
@Repository
public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {
    
    /**
     * Find active session by session ID
     * PRIMARY method for session validation - must be ultra-fast
     * Uses unique index on sessionId
     */
    Optional<AuthSession> findBySessionIdAndStatus(String sessionId, SessionStatus status);
    
    /**
     * Find session by session ID (any status)
     * For session management operations
     */
    Optional<AuthSession> findBySessionId(String sessionId);
    
    /**
     * Find all active sessions for a user
     * Uses composite index on userId, status, expiresAt
     */
    @Query("SELECT s FROM AuthSession s WHERE s.userId = :userId AND s.status = :status AND s.expiresAt > CURRENT_TIMESTAMP")
    List<AuthSession> findActiveSessionsByUserId(@Param("userId") String userId, @Param("status") SessionStatus status);
    
    /**
     * Find all sessions for a user (any status)
     */
    List<AuthSession> findByUserId(String userId);
    
    /**
     * Find sessions by device hash
     * For device tracking and suspicious activity detection
     */
    List<AuthSession> findByDeviceHash(String deviceHash);
    
    /**
     * Count active sessions for user
     * For concurrent session limiting
     */
    @Query("SELECT COUNT(s) FROM AuthSession s WHERE s.userId = :userId AND s.status = 'ACTIVE' AND s.expiresAt > CURRENT_TIMESTAMP")
    long countActiveSessionsByUserId(@Param("userId") String userId);
    
    /**
     * Expire old sessions - automated cleanup
     * Updates status to EXPIRED for sessions past expiry time
     */
    @Modifying
    @Query("UPDATE AuthSession s SET s.status = 'EXPIRED' WHERE s.expiresAt <= :now AND s.status = 'ACTIVE'")
    int expireOldSessions(@Param("now") LocalDateTime now);
    
    /**
     * Revoke all sessions for a user
     * For logout all devices functionality
     */
    @Modifying
    @Query("UPDATE AuthSession s SET s.status = 'REVOKED' WHERE s.userId = :userId AND s.status = 'ACTIVE'")
    int revokeAllUserSessions(@Param("userId") String userId);
    
    /**
     * Revoke sessions except current one
     * For logout other devices functionality
     */
    @Modifying
    @Query("UPDATE AuthSession s SET s.status = 'REVOKED' WHERE s.userId = :userId AND s.sessionId != :currentSessionId AND s.status = 'ACTIVE'")
    int revokeOtherUserSessions(@Param("userId") String userId, @Param("currentSessionId") String currentSessionId);
    
    /**
     * Find sessions that need cleanup
     * For maintenance tasks - remove very old sessions
     */
    @Query("SELECT s FROM AuthSession s WHERE s.createdAt < :cutoffDate AND s.status IN ('EXPIRED', 'REVOKED')")
    List<AuthSession> findSessionsForCleanup(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Delete old sessions for cleanup
     */
    @Modifying
    @Query("DELETE FROM AuthSession s WHERE s.createdAt < :cutoffDate AND s.status IN ('EXPIRED', 'REVOKED')")
    int deleteOldSessions(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Find recent sessions for monitoring
     */
    @Query("SELECT s FROM AuthSession s WHERE s.createdAt >= :since ORDER BY s.createdAt DESC")
    List<AuthSession> findRecentSessions(@Param("since") LocalDateTime since);
    
    /**
     * Find suspicious sessions
     * Sessions marked as suspicious for security review
     */
    List<AuthSession> findByStatus(SessionStatus status);
    
    /**
     * Get session statistics
     */
    @Query("SELECT " +
           "COUNT(s) as totalSessions, " +
           "COUNT(CASE WHEN s.status = 'ACTIVE' AND s.expiresAt > CURRENT_TIMESTAMP THEN 1 END) as activeSessions, " +
           "COUNT(CASE WHEN s.status = 'EXPIRED' THEN 1 END) as expiredSessions, " +
           "COUNT(CASE WHEN s.status = 'REVOKED' THEN 1 END) as revokedSessions, " +
           "COUNT(CASE WHEN s.status = 'SUSPICIOUS' THEN 1 END) as suspiciousSessions " +
           "FROM AuthSession s")
    Object[] getSessionStatistics();
    
    /**
     * Find sessions expiring soon
     * For proactive session extension or warning
     */
    @Query("SELECT s FROM AuthSession s WHERE s.status = 'ACTIVE' AND s.expiresAt BETWEEN CURRENT_TIMESTAMP AND :warningTime")
    List<AuthSession> findSessionsExpiringSoon(@Param("warningTime") LocalDateTime warningTime);
}
