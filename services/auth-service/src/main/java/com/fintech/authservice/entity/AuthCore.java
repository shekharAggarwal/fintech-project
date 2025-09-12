package com.fintech.authservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth_core", indexes = {
        @Index(name = "idx_auth_core_email", columnList = "email", unique = true),
        @Index(name = "idx_auth_core_user_id", columnList = "userId", unique = true),
        @Index(name = "idx_auth_core_status", columnList = "status"),
        @Index(name = "idx_auth_core_status_verified", columnList = "status,emailVerified")
})
public class AuthCore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 36)
    private String userId;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthStatus status = AuthStatus.PENDING_VERIFICATION;

    @Column(nullable = false)
    private Boolean emailVerified = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Soft delete timestamp
    @Column
    private LocalDateTime deletedAt;

    public enum AuthStatus {
        PENDING_VERIFICATION,
        ACTIVE,
        SUSPENDED,
        LOCKED,
        DISABLED,
        DELETED
    }

    // Constructors
    public AuthCore() {
    }

    public AuthCore(String userId, String email) {
        this.userId = userId;
        this.email = email;
    }

    // Business methods
    public boolean isActive() {
        return status == AuthStatus.ACTIVE && deletedAt == null;
    }

    public boolean canAuthenticate() {
        return (status == AuthStatus.ACTIVE || status == AuthStatus.PENDING_VERIFICATION)
                && deletedAt == null;
    }

    public void markAsDeleted() {
        this.status = AuthStatus.DELETED;
        this.deletedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public AuthStatus getStatus() {
        return status;
    }

    public void setStatus(AuthStatus status) {
        this.status = status;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
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

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    @Override
    public String toString() {
        return "AuthCore{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", email='" + email + '\'' +
                ", status=" + status +
                ", emailVerified=" + emailVerified +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthCore)) return false;
        AuthCore authCore = (AuthCore) o;
        return userId != null && userId.equals(authCore.userId);
    }

    @Override
    public int hashCode() {
        return userId != null ? userId.hashCode() : 0;
    }
}
