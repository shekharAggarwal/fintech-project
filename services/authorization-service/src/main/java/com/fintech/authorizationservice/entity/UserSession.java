package com.fintech.authorizationservice.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

@RedisHash("user_session")
public class UserSession implements Serializable {
    
    @Id
    private String sessionId;
    
    private String userEmail;
    private String roleName;
    private Set<String> permissions;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessedAt;
    
    @TimeToLive
    private Long timeToLive = 600L; // 10 minutes default
    
    // Constructors
    public UserSession() {}
    
    public UserSession(String sessionId, String userEmail, String roleName, Set<String> permissions) {
        this.sessionId = sessionId;
        this.userEmail = userEmail;
        this.roleName = roleName;
        this.permissions = permissions;
        this.createdAt = LocalDateTime.now();
        this.lastAccessedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getUserEmail() {
        return userEmail;
    }
    
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
    
    public String getRoleName() {
        return roleName;
    }
    
    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }
    
    public Set<String> getPermissions() {
        return permissions;
    }
    
    public void setPermissions(Set<String> permissions) {
        this.permissions = permissions;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getLastAccessedAt() {
        return lastAccessedAt;
    }
    
    public void setLastAccessedAt(LocalDateTime lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }
    
    public Long getTimeToLive() {
        return timeToLive;
    }
    
    public void setTimeToLive(Long timeToLive) {
        this.timeToLive = timeToLive;
    }
    
    public void updateLastAccessed() {
        this.lastAccessedAt = LocalDateTime.now();
    }
}
