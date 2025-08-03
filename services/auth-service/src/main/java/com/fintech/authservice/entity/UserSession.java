package com.fintech.authservice.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity to track user sessions - allows multiple concurrent sessions per user
 * Essential for production systems to handle session management properly
 */
@Entity
@Table(name = "user_sessions", indexes = {
    @Index(name = "idx_session_id", columnList = "sessionId", unique = true),
    @Index(name = "idx_session_user_id", columnList = "userId"),
    @Index(name = "idx_session_status", columnList = "status"),
    @Index(name = "idx_session_expires_at", columnList = "expiresAt"),
    @Index(name = "idx_session_device_fingerprint", columnList = "deviceFingerprint")
})
public class UserSession {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 255)
    private String sessionId;
    
    @Column(nullable = false, length = 36)
    private String userId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auth_user_id", nullable = false)
    private AuthUser authUser;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionStatus status = SessionStatus.ACTIVE;
    
    @Column(nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(length = 45) // IPv6 support
    private String ipAddress;
    
    @Column(length = 500)
    private String userAgent;
    
    @Column(length = 100)
    private String deviceType; // mobile, desktop, tablet
    
    @Column(length = 255)
    private String deviceFingerprint; // For device tracking
    
    @Column(length = 100)
    private String location; // City, Country
    
    // Security tracking
    @Column
    private LocalDateTime lastAccessedAt;
    
    @Column(nullable = false)
    private Integer accessCount = 0;
    
    @Column(nullable = false)
    private Boolean isTrustedDevice = false;
    
    // Refresh token association
    @Column
    private String refreshTokenHash;
    
    @Column
    private LocalDateTime refreshTokenExpiresAt;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @Column
    private LocalDateTime terminatedAt;
    
    public enum SessionStatus {
        ACTIVE,
        EXPIRED,
        REVOKED,
        SUSPICIOUS
    }
    
    // Constructors
    public UserSession() {}
    
    public UserSession(String sessionId, String userId, AuthUser authUser, 
                      LocalDateTime expiresAt, String ipAddress, String userAgent) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.authUser = authUser;
        this.expiresAt = expiresAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
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
        this.accessCount++;
    }
    
    public void revoke() {
        this.status = SessionStatus.REVOKED;
        this.terminatedAt = LocalDateTime.now();
    }
    
    public void markAsSuspicious() {
        this.status = SessionStatus.SUSPICIOUS;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public AuthUser getAuthUser() { return authUser; }
    public void setAuthUser(AuthUser authUser) { this.authUser = authUser; }
    
    public SessionStatus getStatus() { return status; }
    public void setStatus(SessionStatus status) { this.status = status; }
    
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    
    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    
    public String getDeviceFingerprint() { return deviceFingerprint; }
    public void setDeviceFingerprint(String deviceFingerprint) { this.deviceFingerprint = deviceFingerprint; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }
    public void setLastAccessedAt(LocalDateTime lastAccessedAt) { this.lastAccessedAt = lastAccessedAt; }
    
    public Integer getAccessCount() { return accessCount; }
    public void setAccessCount(Integer accessCount) { this.accessCount = accessCount; }
    
    public Boolean getIsTrustedDevice() { return isTrustedDevice; }
    public void setIsTrustedDevice(Boolean isTrustedDevice) { this.isTrustedDevice = isTrustedDevice; }
    
    public String getRefreshTokenHash() { return refreshTokenHash; }
    public void setRefreshTokenHash(String refreshTokenHash) { this.refreshTokenHash = refreshTokenHash; }
    
    public LocalDateTime getRefreshTokenExpiresAt() { return refreshTokenExpiresAt; }
    public void setRefreshTokenExpiresAt(LocalDateTime refreshTokenExpiresAt) { this.refreshTokenExpiresAt = refreshTokenExpiresAt; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public LocalDateTime getTerminatedAt() { return terminatedAt; }
    public void setTerminatedAt(LocalDateTime terminatedAt) { this.terminatedAt = terminatedAt; }
}
