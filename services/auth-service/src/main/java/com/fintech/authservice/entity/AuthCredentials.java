package com.fintech.authservice.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Optimized credentials entity - ENCRYPTED and SECURE
 * Separated from core auth data for security and performance
 */
@Entity
@Table(name = "auth_credentials", indexes = {
    @Index(name = "idx_auth_creds_core_id", columnList = "authCoreId", unique = true),
    @Index(name = "idx_auth_creds_version", columnList = "authCoreId,version")
})
public class AuthCredentials {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private Long authCoreId; // FK to AuthCore
    
    // Encrypted password hash
    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    private String passwordHash;
    
    // Encrypted salt
    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    private String salt;
    
    @Column
    private LocalDateTime passwordChangedAt;
    
    // Password strength score (0-100)
    @Column(nullable = false)
    private Integer passwordStrength = 0;
    
    // Version for password rotation tracking
    @Column(nullable = false)
    private Integer version = 1;
    
    // Force password change flag
    @Column(nullable = false)
    private Boolean mustChangePassword = false;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // Constructors
    public AuthCredentials() {}
    
    public AuthCredentials(Long authCoreId, String passwordHash, String salt) {
        this.authCoreId = authCoreId;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.passwordChangedAt = LocalDateTime.now();
    }
    
    // Business methods
    public void updatePassword(String newPasswordHash, String newSalt, Integer strength) {
        this.passwordHash = newPasswordHash;
        this.salt = newSalt;
        this.passwordStrength = strength;
        this.passwordChangedAt = LocalDateTime.now();
        this.version++;
        this.mustChangePassword = false;
    }
    
    public boolean isPasswordExpired(int maxDaysValid) {
        if (passwordChangedAt == null) return true;
        return passwordChangedAt.isBefore(LocalDateTime.now().minusDays(maxDaysValid));
    }
    
    public boolean isStrongPassword() {
        return passwordStrength >= 80; // 80% strength threshold
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getAuthCoreId() { return authCoreId; }
    public void setAuthCoreId(Long authCoreId) { this.authCoreId = authCoreId; }
    
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    
    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }
    
    public LocalDateTime getPasswordChangedAt() { return passwordChangedAt; }
    public void setPasswordChangedAt(LocalDateTime passwordChangedAt) { this.passwordChangedAt = passwordChangedAt; }
    
    public Integer getPasswordStrength() { return passwordStrength; }
    public void setPasswordStrength(Integer passwordStrength) { this.passwordStrength = passwordStrength; }
    
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    
    public Boolean getMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(Boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    @Override
    public String toString() {
        return "AuthCredentials{" +
                "id=" + id +
                ", authCoreId=" + authCoreId +
                ", version=" + version +
                ", passwordStrength=" + passwordStrength +
                ", passwordChangedAt=" + passwordChangedAt +
                '}';
    }
}
