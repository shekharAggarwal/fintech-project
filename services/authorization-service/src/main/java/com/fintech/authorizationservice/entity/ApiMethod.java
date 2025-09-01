package com.fintech.authorizationservice.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "api_methods",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"path", "http_method"})
        }
)
public class ApiMethod {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long apiMethodId;
    @Column(nullable = false)
    private String path;
    @Column(name = "http_method", nullable = false)
    private String httpMethod;
    private String description;

    public ApiMethod() {
    }

    public ApiMethod(String path, String httpMethod, String description) {
        this.path = path;
        this.httpMethod = httpMethod;
        this.description = description;
    }

    public Long getApiMethodId() {
        return apiMethodId;
    }

    public void setApiMethodId(Long apiMethodId) {
        this.apiMethodId = apiMethodId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
