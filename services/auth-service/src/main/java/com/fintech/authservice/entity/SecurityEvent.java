package com.fintech.authservice.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity to track security events for auditing and monitoring
 * Critical for production fintech systems for compliance and security monitoring
 */
@Entity
@Table(name = "security_events", indexes = {
    @Index(name = "idx_security_user_id", columnList = "userId"),
    @Index(name = "idx_security_event_type", columnList = "eventType"),
    @Index(name = "idx_security_timestamp", columnList = "timestamp"),
    @Index(name = "idx_security_ip_address", columnList = "ipAddress"),
    @Index(name = "idx_security_risk_level", columnList = "riskLevel"),
    @Index(name = "idx_security_session_id", columnList = "sessionId")
})
public class SecurityEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 36)
    private String userId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auth_user_id", nullable = false)
    private AuthUser authUser;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventType eventType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskLevel riskLevel = RiskLevel.LOW;
    
    @Column(length = 500)
    private String description;
    
    @Column(nullable = false)
    private Boolean successful;
    
    @Column(length = 255)
    private String sessionId;
    
    @Column(length = 45) // IPv6 support
    private String ipAddress;
    
    @Column(length = 500)
    private String userAgent;
    
    @Column(length = 100)
    private String location; // City, Country
    
    @Column(length = 255)
    private String deviceFingerprint;
    
    // Additional context data stored as JSON
    @Column(columnDefinition = "TEXT")
    private String metadata;
    
    @Column(length = 100)
    private String failureReason;
    
    // For linking related events
    @Column(length = 255)
    private String correlationId;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;
    
    public enum EventType {
        // Authentication events
        LOGIN_ATTEMPT,
        LOGIN_SUCCESS,
        LOGIN_FAILURE,
        LOGOUT,
        
        // Password events
        PASSWORD_CHANGE,
        PASSWORD_RESET_REQUEST,
        PASSWORD_RESET_COMPLETED,
        
        // Account events
        ACCOUNT_LOCKED,
        ACCOUNT_UNLOCKED,
        ACCOUNT_SUSPENDED,
        ACCOUNT_REACTIVATED,
        
        // Session events
        SESSION_CREATED,
        SESSION_EXPIRED,
        SESSION_REVOKED,
        CONCURRENT_SESSION_DETECTED,
        
        // Security events
        SUSPICIOUS_ACTIVITY,
        BRUTE_FORCE_DETECTED,
        UNUSUAL_LOCATION,
        NEW_DEVICE_LOGIN,
        
        // MFA events
        MFA_ENABLED,
        MFA_DISABLED,
        MFA_VERIFICATION_SUCCESS,
        MFA_VERIFICATION_FAILURE,
        
        // Email events
        EMAIL_VERIFICATION_SENT,
        EMAIL_VERIFIED,
        EMAIL_CHANGED,
        
        // Token events
        TOKEN_ISSUED,
        TOKEN_REFRESHED,
        TOKEN_REVOKED,
        INVALID_TOKEN_USED,
        
        // Permission events
        PERMISSION_DENIED,
        ROLE_CHANGED,
        
        // Data access
        PROFILE_VIEWED,
        PROFILE_UPDATED,
        SENSITIVE_DATA_ACCESSED
    }
    
    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
    
    // Constructors
    public SecurityEvent() {}
    
    public SecurityEvent(String userId, AuthUser authUser, EventType eventType, 
                        Boolean successful, String description) {
        this.userId = userId;
        this.authUser = authUser;
        this.eventType = eventType;
        this.successful = successful;
        this.description = description;
        this.riskLevel = determineRiskLevel(eventType, successful);
    }
    
    public SecurityEvent(String userId, AuthUser authUser, EventType eventType, 
                        Boolean successful, String description, String ipAddress, 
                        String userAgent, String sessionId) {
        this(userId, authUser, eventType, successful, description);
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.sessionId = sessionId;
    }
    
    // Business methods
    private RiskLevel determineRiskLevel(EventType eventType, Boolean successful) {
        if (!successful) {
            switch (eventType) {
                case LOGIN_FAILURE:
                case MFA_VERIFICATION_FAILURE:
                    return RiskLevel.MEDIUM;
                case BRUTE_FORCE_DETECTED:
                case SUSPICIOUS_ACTIVITY:
                    return RiskLevel.HIGH;
                default:
                    return RiskLevel.LOW;
            }
        }
        
        switch (eventType) {
            case NEW_DEVICE_LOGIN:
            case UNUSUAL_LOCATION:
                return RiskLevel.MEDIUM;
            case ACCOUNT_LOCKED:
            case ACCOUNT_SUSPENDED:
                return RiskLevel.HIGH;
            case PERMISSION_DENIED:
            case INVALID_TOKEN_USED:
                return RiskLevel.MEDIUM;
            default:
                return RiskLevel.LOW;
        }
    }
    
    public boolean isCritical() {
        return riskLevel == RiskLevel.CRITICAL;
    }
    
    public boolean isHighRisk() {
        return riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.CRITICAL;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public AuthUser getAuthUser() { return authUser; }
    public void setAuthUser(AuthUser authUser) { this.authUser = authUser; }
    
    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }
    
    public RiskLevel getRiskLevel() { return riskLevel; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Boolean getSuccessful() { return successful; }
    public void setSuccessful(Boolean successful) { this.successful = successful; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public String getDeviceFingerprint() { return deviceFingerprint; }
    public void setDeviceFingerprint(String deviceFingerprint) { this.deviceFingerprint = deviceFingerprint; }
    
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
