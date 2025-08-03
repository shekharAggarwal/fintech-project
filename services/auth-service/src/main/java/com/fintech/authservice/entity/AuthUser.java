package com.fintech.authservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Core authentication entity - contains only authentication-related data
 * This should be the primary entity for all authentication operations
 */
@Entity
@Table(name = "auth_users", indexes = {
    @Index(name = "idx_auth_user_email", columnList = "email", unique = true),
    @Index(name = "idx_auth_user_id", columnList = "userId", unique = true),
    @Index(name = "idx_auth_user_shard_key", columnList = "shardKey"),
    @Index(name = "idx_auth_user_status", columnList = "status"),
    @Index(name = "idx_auth_user_last_login", columnList = "lastLoginAt")
})
public class AuthUser {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Unique user identifier for sharding and cross-service communication
    @Column(nullable = false, unique = true, length = 36)
    private String userId;
    
    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    @Column(nullable = false, unique = true, length = 150)
    private String email;
    
    @NotBlank(message = "Password is required")
    @Column(nullable = false)
    private String passwordHash;
    
    // Password security fields
    @Column(nullable = false)
    private String salt;
    
    @Column(nullable = false)
    private Integer passwordStrength = 0; // 0-100 scale
    
    @Column
    private LocalDateTime passwordChangedAt;
    
    @Column(nullable = false)
    private Boolean passwordMustChange = false;
    
    // Account status and security
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthStatus status = AuthStatus.PENDING_VERIFICATION;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.ACCOUNT_HOLDER;
    
    // Security tracking
    @Column(nullable = false)
    private Integer failedLoginAttempts = 0;
    
    @Column
    private LocalDateTime lockedUntil;
    
    @Column
    private LocalDateTime lastLoginAt;
    
    @Column(length = 45) // IPv6 support
    private String lastLoginIp;
    
    @Column(length = 500)
    private String lastLoginUserAgent;
    
    // Email verification
    @Column(nullable = false)
    private Boolean emailVerified = false;
    
    @Column
    private String emailVerificationToken;
    
    @Column
    private LocalDateTime emailVerificationTokenExpiresAt;
    
    // MFA fields
    @Column(nullable = false)
    private Boolean mfaEnabled = false;
    
    @Column
    private String mfaSecret;
    
    @Enumerated(EnumType.STRING)
    private MfaMethod preferredMfaMethod;
    
    // Session management
    @Column
    private String currentSessionId;
    
    @Column
    private LocalDateTime sessionExpiresAt;
    
    // Refresh token management
    @Column
    private String refreshTokenHash; // Store hash, not plain token
    
    @Column
    private LocalDateTime refreshTokenExpiresAt;
    
    @Column(nullable = false)
    private Integer refreshTokenVersion = 0; // For token invalidation
    
    // Audit fields
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @Column
    private LocalDateTime deletedAt; // Soft delete
    
    // Sharding key
    @Column(nullable = false)
    private Integer shardKey;
    
    // Relationships
    @OneToMany(mappedBy = "authUser", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<UserSession> sessions = new ArrayList<>();
    
    @OneToMany(mappedBy = "authUser", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SecurityEvent> securityEvents = new ArrayList<>();
    
    public enum AuthStatus {
        PENDING_VERIFICATION,
        ACTIVE,
        SUSPENDED,
        LOCKED,
        DISABLED,
        DELETED
    }
    
    public enum UserRole {
        ACCOUNT_HOLDER("ROLE_ACCOUNT_HOLDER", "Account Holder"),
        SALES_PERSON("ROLE_SALES_PERSON", "Sales Person"), 
        MANAGER("ROLE_MANAGER", "Manager"),
        ADMIN("ROLE_ADMIN", "Administrator"),
        SYSTEM("ROLE_SYSTEM", "System");
        
        private final String authority;
        private final String displayName;
        
        UserRole(String authority, String displayName) {
            this.authority = authority;
            this.displayName = displayName;
        }
        
        public String getAuthority() {
            return authority;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public enum MfaMethod {
        TOTP, // Time-based One-Time Password
        SMS,
        EMAIL,
        HARDWARE_TOKEN
    }
    
    // Constructors
    public AuthUser() {}
    
    public AuthUser(String userId, String email, String passwordHash, String salt) {
        this.userId = userId;
        this.email = email;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.shardKey = userId.hashCode() % 3 + 1;
    }
    
    // Security methods
    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }
    
    public boolean isActive() {
        return status == AuthStatus.ACTIVE && !isLocked() && deletedAt == null;
    }
    
    public boolean requiresMfaVerification() {
        return mfaEnabled && preferredMfaMethod != null;
    }
    
    public void incrementFailedAttempts() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= 5) {
            this.lockedUntil = LocalDateTime.now().plusMinutes(30);
        }
    }
    
    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }
    
    public void updateLastLogin(String ip, String userAgent) {
        this.lastLoginAt = LocalDateTime.now();
        this.lastLoginIp = ip;
        this.lastLoginUserAgent = userAgent;
        resetFailedAttempts();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    
    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }
    
    public Integer getPasswordStrength() { return passwordStrength; }
    public void setPasswordStrength(Integer passwordStrength) { this.passwordStrength = passwordStrength; }
    
    public LocalDateTime getPasswordChangedAt() { return passwordChangedAt; }
    public void setPasswordChangedAt(LocalDateTime passwordChangedAt) { this.passwordChangedAt = passwordChangedAt; }
    
    public Boolean getPasswordMustChange() { return passwordMustChange; }
    public void setPasswordMustChange(Boolean passwordMustChange) { this.passwordMustChange = passwordMustChange; }
    
    public AuthStatus getStatus() { return status; }
    public void setStatus(AuthStatus status) { this.status = status; }
    
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    
    public Integer getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(Integer failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }
    
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }
    
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    
    public String getLastLoginIp() { return lastLoginIp; }
    public void setLastLoginIp(String lastLoginIp) { this.lastLoginIp = lastLoginIp; }
    
    public String getLastLoginUserAgent() { return lastLoginUserAgent; }
    public void setLastLoginUserAgent(String lastLoginUserAgent) { this.lastLoginUserAgent = lastLoginUserAgent; }
    
    public Boolean getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }
    
    public String getEmailVerificationToken() { return emailVerificationToken; }
    public void setEmailVerificationToken(String emailVerificationToken) { this.emailVerificationToken = emailVerificationToken; }
    
    public LocalDateTime getEmailVerificationTokenExpiresAt() { return emailVerificationTokenExpiresAt; }
    public void setEmailVerificationTokenExpiresAt(LocalDateTime emailVerificationTokenExpiresAt) { this.emailVerificationTokenExpiresAt = emailVerificationTokenExpiresAt; }
    
    public Boolean getMfaEnabled() { return mfaEnabled; }
    public void setMfaEnabled(Boolean mfaEnabled) { this.mfaEnabled = mfaEnabled; }
    
    public String getMfaSecret() { return mfaSecret; }
    public void setMfaSecret(String mfaSecret) { this.mfaSecret = mfaSecret; }
    
    public MfaMethod getPreferredMfaMethod() { return preferredMfaMethod; }
    public void setPreferredMfaMethod(MfaMethod preferredMfaMethod) { this.preferredMfaMethod = preferredMfaMethod; }
    
    public String getCurrentSessionId() { return currentSessionId; }
    public void setCurrentSessionId(String currentSessionId) { this.currentSessionId = currentSessionId; }
    
    public LocalDateTime getSessionExpiresAt() { return sessionExpiresAt; }
    public void setSessionExpiresAt(LocalDateTime sessionExpiresAt) { this.sessionExpiresAt = sessionExpiresAt; }
    
    public String getRefreshTokenHash() { return refreshTokenHash; }
    public void setRefreshTokenHash(String refreshTokenHash) { this.refreshTokenHash = refreshTokenHash; }
    
    public LocalDateTime getRefreshTokenExpiresAt() { return refreshTokenExpiresAt; }
    public void setRefreshTokenExpiresAt(LocalDateTime refreshTokenExpiresAt) { this.refreshTokenExpiresAt = refreshTokenExpiresAt; }
    
    public Integer getRefreshTokenVersion() { return refreshTokenVersion; }
    public void setRefreshTokenVersion(Integer refreshTokenVersion) { this.refreshTokenVersion = refreshTokenVersion; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    
    public Integer getShardKey() { return shardKey; }
    public void setShardKey(Integer shardKey) { this.shardKey = shardKey; }
    
    public List<UserSession> getSessions() { return sessions; }
    public void setSessions(List<UserSession> sessions) { this.sessions = sessions; }
    
    public List<SecurityEvent> getSecurityEvents() { return securityEvents; }
    public void setSecurityEvents(List<SecurityEvent> securityEvents) { this.securityEvents = securityEvents; }
}
