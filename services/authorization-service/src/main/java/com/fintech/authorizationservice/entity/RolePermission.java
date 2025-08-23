package com.fintech.authorizationservice.entity;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;

@Entity
@Table(name = "role_permissions")
public class RolePermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "method_id")
    private ApiMethod apiMethodId;

    private boolean allowed;

    @Nullable
    private String limitType;

    @Nullable
    private Integer limitValue;

    public RolePermission() {
    }

    public RolePermission(Role role, ApiMethod apiMethodId, boolean allowed, @Nullable String limitType, @Nullable Integer limitValue) {
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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public ApiMethod getApiMethodId() {
        return apiMethodId;
    }

    public void setApiMethodId(ApiMethod apiMethodId) {
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
