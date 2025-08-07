package com.fintech.authservice.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Optimized session entity - FAST session lookups
 * Minimal data for quick session validation
 */
@Entity
@Table(name = "auth_sessions", indexes = {
    @Index(name = "idx_auth_sessions_session_id", columnList = "sessionId", unique = true),
    @Index(name = "idx_auth_sessions_user_id", columnList = "userId"),
    @Index(name = "idx_auth_sessions_expires_at", columnList = "expiresAt"),
    @Index(name = "idx_auth_sessions_status", columnList = "status"),
    @Index(name = "idx_auth_sessions_device_hash", columnList = "deviceHash"),
    @Index(name = "idx_auth_sessions_user_active", columnList = "userId,status,expiresAt")
})
public class AuthSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 255)
    private String sessionId;
    
    @Column(nullable = false, length = 36)
    private String userId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status = SessionStatus.ACTIVE;
    
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    
    // Device fingerprint hash for security
    @Column(length = 255)
    private String deviceHash;
    
    @Column
    private LocalDateTime lastAccessedAt;
    
    // Refresh token hash (encrypted)
    @Column(length = 255)
    private String refreshTokenHash;
    
    @Column
    private LocalDateTime refreshTokenExpiresAt;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    public enum SessionStatus {
        ACTIVE,
        EXPIRED,
        REVOKED,
        SUSPICIOUS
    }
    
    // Constructors
    public AuthSession() {}
    
    public AuthSession(String sessionId, String userId, LocalDateTime expiresAt, String deviceHash) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.expiresAt = expiresAt;
        this.deviceHash = deviceHash;
        this.lastAccessedAt = LocalDateTime.now();
    }
    
    // Business methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean isActive() {
        return status == SessionStatus.ACTIVE && !isExpired();
    }
    
    public void markAsAccessed() {
        this.lastAccessedAt = LocalDateTime.now();
    }
    
    public void revoke() {
        this.status = SessionStatus.REVOKED;
    }
    
    public void markAsSuspicious() {
        this.status = SessionStatus.SUSPICIOUS;
    }
    
    public void extendSession(int minutesToAdd) {
        if (isActive()) {
            this.expiresAt = this.expiresAt.plusMinutes(minutesToAdd);
        }
    }
    
    public boolean isRefreshTokenValid() {
        return refreshTokenHash != null && 
               refreshTokenExpiresAt != null && 
               refreshTokenExpiresAt.isAfter(LocalDateTime.now());
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }
    
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    
    public String getDeviceHash() { return deviceHash; }
    public void setDeviceHash(String deviceHash) { this.deviceHash = deviceHash; }
    
    public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }
    public void setLastAccessedAt(LocalDateTime lastAccessedAt) { this.lastAccessedAt = lastAccessedAt; }
    
    public String getRefreshTokenHash() { return refreshTokenHash; }
    public void setRefreshTokenHash(String refreshTokenHash) { this.refreshTokenHash = refreshTokenHash; }
    
    public LocalDateTime getRefreshTokenExpiresAt() { return refreshTokenExpiresAt; }
    public void setRefreshTokenExpiresAt(LocalDateTime refreshTokenExpiresAt) { this.refreshTokenExpiresAt = refreshTokenExpiresAt; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    @Override
    public String toString() {
        return "AuthSession{" +
                "id=" + id +
                ", sessionId='" + sessionId + '\'' +
                ", userId='" + userId + '\'' +
                ", status=" + status +
                ", expiresAt=" + expiresAt +
                ", lastAccessedAt=" + lastAccessedAt +
                '}';
    }
}
