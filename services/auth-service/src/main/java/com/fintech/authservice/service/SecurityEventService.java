package com.fintech.authservice.service;

import com.fintech.authservice.entity.AuthUser;
import com.fintech.authservice.entity.SecurityEvent;
import com.fintech.authservice.repository.SecurityEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for logging and managing security events
 * Critical for production fintech systems for audit and compliance
 * Integrates with Splunk for centralized logging and real-time monitoring
 */
@Service
@Transactional
public class SecurityEventService {

    private final SecurityEventRepository securityEventRepository;
    private final SplunkLoggingService splunkLoggingService;

    public SecurityEventService(SecurityEventRepository securityEventRepository,
                               SplunkLoggingService splunkLoggingService) {
        this.securityEventRepository = securityEventRepository;
        this.splunkLoggingService = splunkLoggingService;
    }

    /**
     * Log a security event
     */
    public void logSecurityEvent(String userId, AuthUser authUser, SecurityEvent.EventType eventType,
                                boolean successful, String description, String ipAddress, 
                                String userAgent, String sessionId) {
        
        SecurityEvent event = new SecurityEvent(userId, authUser, eventType, successful, description);
        event.setIpAddress(ipAddress);
        event.setUserAgent(userAgent);
        event.setSessionId(sessionId);
        event.setCorrelationId(UUID.randomUUID().toString());
        
        // Set additional metadata based on event type
        switch (eventType) {
            case BRUTE_FORCE_DETECTED:
            case SUSPICIOUS_ACTIVITY:
                event.setRiskLevel(SecurityEvent.RiskLevel.HIGH);
                break;
            case NEW_DEVICE_LOGIN:
            case UNUSUAL_LOCATION:
                event.setRiskLevel(SecurityEvent.RiskLevel.MEDIUM);
                break;
            case LOGIN_FAILURE:
            case MFA_VERIFICATION_FAILURE:
                event.setRiskLevel(successful ? SecurityEvent.RiskLevel.LOW : SecurityEvent.RiskLevel.MEDIUM);
                break;
            default:
                event.setRiskLevel(SecurityEvent.RiskLevel.LOW);
        }
        
        // Save to database for immediate queries and compliance
        SecurityEvent savedEvent = securityEventRepository.save(event);
        
        // Send to Splunk for centralized logging and real-time monitoring
        splunkLoggingService.logSecurityEvent(savedEvent);
        
        // Trigger alerts for high-risk events
        if (event.isHighRisk()) {
            triggerSecurityAlert(event);
        }
    }

    /**
     * Log a security event with failure reason
     */
    public void logSecurityEvent(String userId, AuthUser authUser, SecurityEvent.EventType eventType,
                                boolean successful, String description, String ipAddress, 
                                String userAgent, String sessionId, String failureReason) {
        
        logSecurityEvent(userId, authUser, eventType, successful, description, ipAddress, userAgent, sessionId);
        
        if (!successful && failureReason != null) {
            SecurityEvent event = securityEventRepository.findBySessionId(sessionId)
                    .stream()
                    .filter(e -> e.getEventType() == eventType)
                    .findFirst()
                    .orElse(null);
            
            if (event != null) {
                event.setFailureReason(failureReason);
                securityEventRepository.save(event);
            }
        }
    }

    /**
     * Check for suspicious activity patterns
     */
    public boolean isSuspiciousActivity(String userId, String ipAddress) {
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);
        
        // Check for multiple failed logins
        long failedAttempts = securityEventRepository.countByUserIdAndEventTypeAndTimestampAfter(
            userId, SecurityEvent.EventType.LOGIN_FAILURE, oneDayAgo);
        
        if (failedAttempts >= 5) {
            return true;
        }
        
        // Check for failed attempts from this IP
        long ipFailedAttempts = securityEventRepository.countFailedEventsByIpAndTimestampAfter(
            ipAddress, oneDayAgo);
        
        return ipFailedAttempts >= 10;
    }

    /**
     * Check if IP address has brute force activity
     */
    public boolean isBruteForceDetected(String ipAddress) {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        
        long failedAttempts = securityEventRepository.countFailedEventsByIpAndTimestampAfter(
            ipAddress, oneHourAgo);
        
        return failedAttempts >= 5;
    }

    /**
     * Get recent security events for a user
     */
    public java.util.List<SecurityEvent> getRecentSecurityEvents(String userId, int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return securityEventRepository.findByUserIdAndTimestampAfter(userId, since);
    }

    /**
     * Get high-risk events
     */
    public java.util.List<SecurityEvent> getHighRiskEvents(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return securityEventRepository.findHighRiskEventsAfter(since);
    }

    /**
     * Trigger security alert for high-risk events
     */
    private void triggerSecurityAlert(SecurityEvent event) {
        // In production, this would:
        // 1. Send alerts to security team
        // 2. Update monitoring systems
        // 3. Potentially auto-suspend accounts
        // 4. Log to SIEM systems
        
        System.out.println("SECURITY ALERT: " + event.getEventType() + 
                          " for user " + event.getUserId() + 
                          " with risk level " + event.getRiskLevel());
    }

    /**
     * Clean up old security events (for GDPR compliance)
     */
    @Transactional
    public void cleanupOldEvents(int retentionMonths) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusMonths(retentionMonths);
        
        // In production, implement batch deletion
        // securityEventRepository.deleteEventsOlderThan(cutoffDate);
    }
}
