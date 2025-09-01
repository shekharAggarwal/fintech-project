package com.fintech.authorizationservice.entity;

import jakarta.annotation.Nullable;
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

    @Nullable
    private String limitType;

    @Nullable
    private Integer limitValue;

    public RolePermission() {
    }

    public RolePermission(Long role, Long apiMethodId, boolean allowed, @Nullable String limitType, @Nullable Integer limitValue) {
        this.role = role;
        this.apiMethodId = apiMethodId;
        this.allowed = allowed;
        this.limitType = limitType;
        this.limitValue = limitValue;
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

    @Nullable
    public String getLimitType() {
        return limitType;
    }

    public void setLimitType(@Nullable String limitType) {
        this.limitType = limitType;
    }

    @Nullable
    public Integer getLimitValue() {
        return limitValue;
    }

    public void setLimitValue(@Nullable Integer limitValue) {
        this.limitValue = limitValue;
    }
}
