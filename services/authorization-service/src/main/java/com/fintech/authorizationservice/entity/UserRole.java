package com.fintech.authorizationservice.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_roles")
public class UserRole {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private String userId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;
    
    @Column(name = "created_at")
    private Long createdAt;

    public UserRole() {
        this.createdAt = System.currentTimeMillis();
    }

    public UserRole(String userId, Role role) {
        this.userId = userId;
        this.role = role;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and setters
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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "UserRole{" +
                "id=" + id +
                ", userId='" + userId + '\'' +
                ", role=" + (role != null ? role.getName() : null) +
                ", createdAt=" + createdAt +
                '}';
    }
}
