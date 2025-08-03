package com.fintech.authservice.repository;

import com.fintech.authservice.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    
    Optional<UserSession> findBySessionId(String sessionId);
    
    @Query("SELECT s FROM UserSession s WHERE s.sessionId = :sessionId AND s.status = 'ACTIVE'")
    Optional<UserSession> findActiveSessionById(@Param("sessionId") String sessionId);
    
    @Query("SELECT s FROM UserSession s WHERE s.userId = :userId AND s.status = 'ACTIVE' ORDER BY s.lastAccessedAt DESC")
    List<UserSession> findActiveSessionsByUserId(@Param("userId") String userId);
    
    @Query("SELECT s FROM UserSession s WHERE s.userId = :userId ORDER BY s.lastAccessedAt DESC")
    List<UserSession> findAllSessionsByUserId(@Param("userId") String userId);
    
    @Query("SELECT s FROM UserSession s WHERE s.expiresAt < :now AND s.status = 'ACTIVE'")
    List<UserSession> findExpiredSessions(@Param("now") LocalDateTime now);
    
    @Query("SELECT s FROM UserSession s WHERE s.userId = :userId AND s.deviceFingerprint = :deviceFingerprint AND s.status = 'ACTIVE'")
    List<UserSession> findActiveSessionsByUserAndDevice(@Param("userId") String userId, @Param("deviceFingerprint") String deviceFingerprint);
    
    @Query("SELECT s FROM UserSession s WHERE s.userId = :userId AND s.ipAddress = :ipAddress AND s.status = 'ACTIVE'")
    List<UserSession> findActiveSessionsByUserAndIp(@Param("userId") String userId, @Param("ipAddress") String ipAddress);
    
    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.userId = :userId AND s.status = 'ACTIVE'")
    long countActiveSessionsByUserId(@Param("userId") String userId);
    
    @Query("SELECT s FROM UserSession s WHERE s.refreshTokenHash = :refreshTokenHash AND s.status = 'ACTIVE'")
    Optional<UserSession> findByRefreshTokenHash(@Param("refreshTokenHash") String refreshTokenHash);
    
    @Query("SELECT s FROM UserSession s WHERE s.lastAccessedAt < :cutoffTime AND s.status = 'ACTIVE'")
    List<UserSession> findInactiveSessions(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    @Query("SELECT s FROM UserSession s WHERE s.userId = :userId AND s.isTrustedDevice = false AND s.status = 'ACTIVE'")
    List<UserSession> findUntrustedSessionsByUserId(@Param("userId") String userId);
    
    @Query("SELECT DISTINCT s.ipAddress FROM UserSession s WHERE s.userId = :userId AND s.status = 'ACTIVE'")
    List<String> findActiveIpAddressesByUserId(@Param("userId") String userId);
    
    @Query("SELECT s FROM UserSession s WHERE s.status = 'SUSPICIOUS'")
    List<UserSession> findSuspiciousSessions();
}
