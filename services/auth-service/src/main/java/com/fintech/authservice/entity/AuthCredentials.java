package com.fintech.authservice.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth_credentials", indexes = {
        @Index(name = "idx_auth_creds_core_id", columnList = "authCoreId", unique = true),
})
public class AuthCredentials {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JoinColumn(name = "authCoreId", referencedColumnName = "id")
    @OneToOne(fetch = FetchType.LAZY)
    private AuthCore authCoreId; // FK to AuthCore

    // Encrypted password hash
    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    private String passwordHash;

    // Encrypted salt
    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    private String salt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Constructors
    public AuthCredentials() {
    }

    public AuthCredentials(AuthCore authCoreId, String passwordHash, String salt) {
        this.authCoreId = authCoreId;
        this.passwordHash = passwordHash;
        this.salt = salt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AuthCore getAuthCoreId() {
        return authCoreId;
    }

    public void setAuthCoreId(AuthCore authCoreId) {
        this.authCoreId = authCoreId;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "";
    }
}
