package com.fintech.authservice.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Optimized security tracking entity
 * Focused on security-related data only for better performance
 */
@Entity
@Table(name = "auth_security", indexes = {
    @Index(name = "idx_auth_security_user_id", columnList = "userId", unique = true),
    @Index(name = "idx_auth_security_locked_until", columnList = "lockedUntil"),
    @Index(name = "idx_auth_security_risk_score", columnList = "riskScore"),
    @Index(name = "idx_auth_security_last_login", columnList = "lastLoginAt")
})
public class AuthSecurity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 36)
    private String userId;
    
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
    
    // Risk score calculated from various factors (0-100)
    @Column(nullable = false)
    private Integer riskScore = 0;
    
    // Geographic location of last login
    @Column(length = 100)
    private String lastLoginLocation;
    
    // Device fingerprint hash
    @Column(length = 255)
    private String lastDeviceFingerprint;
    
    // Suspicious activity counter
    @Column(nullable = false)
    private Integer suspiciousActivityCount = 0;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // Constructors
    public AuthSecurity() {}
    
    public AuthSecurity(String userId) {
        this.userId = userId;
    }
    
    // Business methods
    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }
    
    public void incrementFailedAttempts() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= 5) {
            this.lockedUntil = LocalDateTime.now().plusMinutes(30);
        }
        updateRiskScore();
    }
    
    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        updateRiskScore();
    }
    
    public void recordSuccessfulLogin(String ip, String userAgent, String location, String deviceFingerprint) {
        this.lastLoginAt = LocalDateTime.now();
        this.lastLoginIp = ip;
        this.lastLoginUserAgent = userAgent;
        this.lastLoginLocation = location;
        this.lastDeviceFingerprint = deviceFingerprint;
        resetFailedAttempts();
    }
    
    public void incrementSuspiciousActivity() {
        this.suspiciousActivityCount++;
        updateRiskScore();
    }
    
    private void updateRiskScore() {
        int score = 0;
        
        // Failed attempts contribute to risk
        score += Math.min(failedLoginAttempts * 20, 60);
        
        // Suspicious activity
        score += Math.min(suspiciousActivityCount * 10, 30);
        
        // Account locked
        if (isLocked()) {
            score += 40;
        }
        
        this.riskScore = Math.min(score, 100);
    }
    
    public boolean isHighRisk() {
        return riskScore >= 70;
    }
    
    public boolean isMediumRisk() {
        return riskScore >= 40 && riskScore < 70;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public Integer getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(Integer failedLoginAttempts) { 
        this.failedLoginAttempts = failedLoginAttempts;
        updateRiskScore();
    }
    
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }
    
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    
    public String getLastLoginIp() { return lastLoginIp; }
    public void setLastLoginIp(String lastLoginIp) { this.lastLoginIp = lastLoginIp; }
    
    public String getLastLoginUserAgent() { return lastLoginUserAgent; }
    public void setLastLoginUserAgent(String lastLoginUserAgent) { this.lastLoginUserAgent = lastLoginUserAgent; }
    
    public Integer getRiskScore() { return riskScore; }
    public void setRiskScore(Integer riskScore) { this.riskScore = riskScore; }
    
    public String getLastLoginLocation() { return lastLoginLocation; }
    public void setLastLoginLocation(String lastLoginLocation) { this.lastLoginLocation = lastLoginLocation; }
    
    public String getLastDeviceFingerprint() { return lastDeviceFingerprint; }
    public void setLastDeviceFingerprint(String lastDeviceFingerprint) { this.lastDeviceFingerprint = lastDeviceFingerprint; }
    
    public Integer getSuspiciousActivityCount() { return suspiciousActivityCount; }
    public void setSuspiciousActivityCount(Integer suspiciousActivityCount) { 
        this.suspiciousActivityCount = suspiciousActivityCount;
        updateRiskScore();
    }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
