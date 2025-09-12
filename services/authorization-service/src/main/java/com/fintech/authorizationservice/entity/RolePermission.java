package com.fintech.authorizationservice.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "role_permissions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"role_id", "method_id"})
        }
)
public class RolePermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false)
    private Long role;

    @Column(name = "method_id", nullable = false)
    private Long apiMethodId;

    private boolean allowed;


    public RolePermission() {
    }

    public RolePermission(Long role, Long apiMethodId, boolean allowed) {
        this.role = role;
        this.apiMethodId = apiMethodId;
        this.allowed = allowed;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRole() {
        return role;
    }

    public void setRole(Long role) {
        this.role = role;
    }

    public Long getApiMethodId() {
        return apiMethodId;
    }

    public void setApiMethodId(Long apiMethodId) {
        this.apiMethodId = apiMethodId;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }
}
