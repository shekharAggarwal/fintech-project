package com.fintech.authorizationservice.entity;


import jakarta.persistence.*;

@Entity
@Table(
        name = "field_access",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"role_id", "resource_type"})
        }
)
public class FieldAccess {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_id", nullable = false)
    private Long role;

    @Column(nullable = false)
    private String resourceType;

    @Column(columnDefinition = "jsonb")
    private String allowedFields;

    @Column(columnDefinition = "jsonb")
    private String config;

    public FieldAccess() {
    }

    public FieldAccess(Long role, String resourceType, String allowedFields, String config) {
        this.role = role;
        this.resourceType = resourceType;
        this.allowedFields = allowedFields;
        this.config = config;
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

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getAllowedFields() {
        return allowedFields;
    }

    public void setAllowedFields(String allowedFields) {
        this.allowedFields = allowedFields;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }
}