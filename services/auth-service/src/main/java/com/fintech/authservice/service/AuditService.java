package com.fintech.authservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditService {
    
    private static final Logger auditLogger = LoggerFactory.getLogger("AUDIT");
    private final RabbitTemplate rabbitTemplate;
    
    public AuditService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }
    
    public void logAuthEvent(AuditEventType eventType, String userId, String ipAddress, 
                           String userAgent, boolean success, String details) {
        
        AuditEvent event = AuditEvent.builder()
                .eventType(eventType)
                .userId(userId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .success(success)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
        
        // Log to structured logs
        logEvent(event);
        
        // Send to audit queue for persistence
        try {
            rabbitTemplate.convertAndSend("audit.exchange", "audit.auth", event);
        } catch (Exception e) {
            auditLogger.error("Failed to send audit event to queue", e);
        }
    }
    
    private void logEvent(AuditEvent event) {
        try {
            // Set MDC for structured logging
            MDC.put("eventType", event.getEventType().name());
            MDC.put("userId", event.getUserId());
            MDC.put("ipAddress", event.getIpAddress());
            MDC.put("success", String.valueOf(event.isSuccess()));
            
            if (event.isSuccess()) {
                auditLogger.info("Auth event: {} for user: {} from IP: {} - SUCCESS. Details: {}", 
                    event.getEventType(), event.getUserId(), event.getIpAddress(), event.getDetails());
            } else {
                auditLogger.warn("Auth event: {} for user: {} from IP: {} - FAILED. Details: {}", 
                    event.getEventType(), event.getUserId(), event.getIpAddress(), event.getDetails());
            }
        } finally {
            MDC.clear();
        }
    }
    
    public enum AuditEventType {
        LOGIN_ATTEMPT,
        LOGIN_SUCCESS,
        LOGIN_FAILED,
        LOGOUT,
        REGISTRATION_ATTEMPT,
        REGISTRATION_SUCCESS,
        REGISTRATION_FAILED,
        PASSWORD_CHANGE,
        PASSWORD_RESET_REQUEST,
        PASSWORD_RESET_SUCCESS,
        TOKEN_REFRESH,
        ACCOUNT_LOCKED,
        ACCOUNT_DELETED,
        SESSION_EXPIRED,
        UNAUTHORIZED_ACCESS_ATTEMPT
    }
    
    public static class AuditEvent {
        private AuditEventType eventType;
        private String userId;
        private String ipAddress;
        private String userAgent;
        private boolean success;
        private String details;
        private LocalDateTime timestamp;
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private AuditEvent event = new AuditEvent();
            
            public Builder eventType(AuditEventType eventType) {
                event.eventType = eventType;
                return this;
            }
            
            public Builder userId(String userId) {
                event.userId = userId;
                return this;
            }
            
            public Builder ipAddress(String ipAddress) {
                event.ipAddress = ipAddress;
                return this;
            }
            
            public Builder userAgent(String userAgent) {
                event.userAgent = userAgent;
                return this;
            }
            
            public Builder success(boolean success) {
                event.success = success;
                return this;
            }
            
            public Builder details(String details) {
                event.details = details;
                return this;
            }
            
            public Builder timestamp(LocalDateTime timestamp) {
                event.timestamp = timestamp;
                return this;
            }
            
            public AuditEvent build() {
                return event;
            }
        }
        
        // Getters
        public AuditEventType getEventType() { return eventType; }
        public String getUserId() { return userId; }
        public String getIpAddress() { return ipAddress; }
        public String getUserAgent() { return userAgent; }
        public boolean isSuccess() { return success; }
        public String getDetails() { return details; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}
