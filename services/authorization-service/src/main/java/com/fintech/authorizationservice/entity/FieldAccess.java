package com.fintech.authorizationservice.entity;


import com.fintech.authorizationservice.converter.StringListJsonConverter;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

import java.util.List;

@Entity
@Table(name = "field_access")
public class FieldAccess {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    private String resourceType; // e.g., "user", "account"


    @Column(columnDefinition = "jsonb")
    @Convert(converter = StringListJsonConverter.class)
    private List<String> allowedFields;


    @Column(columnDefinition = "jsonb")
    private String config;

    public FieldAccess() {
    }

    public FieldAccess(Role role, String resourceType, List<String> allowedFields, String config) {
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

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public List<String> getAllowedFields() {
        return allowedFields;
    }

    public void setAllowedFields(List<String> allowedFields) {
        this.allowedFields = allowedFields;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
    }
}