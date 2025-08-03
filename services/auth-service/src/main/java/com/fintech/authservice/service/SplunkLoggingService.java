package com.fintech.authservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.authservice.entity.AuthUser;
import com.fintech.authservice.entity.SecurityEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for sending security events and logs to Splunk
 * Uses HTTP Event Collector (HEC) for real-time event streaming
 */
@Service
public class SplunkLoggingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${splunk.hec.url:http://localhost:8088/services/collector}")
    private String splunkHecUrl;

    @Value("${splunk.hec.token:fintech-hec-token-2024}")
    private String splunkHecToken;

    @Value("${splunk.index:fintech}")
    private String splunkIndex;

    @Value("${spring.application.name:auth-service}")
    private String serviceName;

    public SplunkLoggingService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Send security event to Splunk asynchronously
     */
    @Async
    public void logSecurityEvent(SecurityEvent event) {
        try {
            Map<String, Object> splunkEvent = createSplunkEvent(
                "security_event",
                createSecurityEventPayload(event)
            );
            
            sendToSplunk(splunkEvent);
        } catch (Exception e) {
            // Log locally if Splunk is unavailable
            System.err.println("Failed to send security event to Splunk: " + e.getMessage());
        }
    }

    /**
     * Send authentication event to Splunk
     */
    @Async
    public void logAuthEvent(String userId, String eventType, boolean success, 
                           String ipAddress, String userAgent, String details) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("user_id", userId);
            eventData.put("event_type", eventType);
            eventData.put("success", success);
            eventData.put("ip_address", ipAddress);
            eventData.put("user_agent", userAgent);
            eventData.put("details", details);
            eventData.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            Map<String, Object> splunkEvent = createSplunkEvent("auth_event", eventData);
            sendToSplunk(splunkEvent);
        } catch (Exception e) {
            System.err.println("Failed to send auth event to Splunk: " + e.getMessage());
        }
    }

    /**
     * Send session event to Splunk
     */
    @Async
    public void logSessionEvent(String sessionId, String userId, String eventType, 
                              String ipAddress, String deviceFingerprint, Map<String, Object> metadata) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("session_id", sessionId);
            eventData.put("user_id", userId);
            eventData.put("event_type", eventType);
            eventData.put("ip_address", ipAddress);
            eventData.put("device_fingerprint", deviceFingerprint);
            eventData.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            if (metadata != null) {
                eventData.put("metadata", metadata);
            }

            Map<String, Object> splunkEvent = createSplunkEvent("session_event", eventData);
            sendToSplunk(splunkEvent);
        } catch (Exception e) {
            System.err.println("Failed to send session event to Splunk: " + e.getMessage());
        }
    }

    /**
     * Send application performance metrics to Splunk
     */
    @Async
    public void logPerformanceMetric(String operation, long duration, boolean success, 
                                   Map<String, Object> additionalData) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("operation", operation);
            eventData.put("duration_ms", duration);
            eventData.put("success", success);
            eventData.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            if (additionalData != null) {
                eventData.putAll(additionalData);
            }

            Map<String, Object> splunkEvent = createSplunkEvent("performance_metric", eventData);
            sendToSplunk(splunkEvent);
        } catch (Exception e) {
            System.err.println("Failed to send performance metric to Splunk: " + e.getMessage());
        }
    }

    /**
     * Send business event to Splunk
     */
    @Async
    public void logBusinessEvent(String eventType, String userId, Map<String, Object> eventData) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("event_type", eventType);
            payload.put("user_id", userId);
            payload.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            if (eventData != null) {
                payload.putAll(eventData);
            }

            Map<String, Object> splunkEvent = createSplunkEvent("business_event", payload);
            sendToSplunk(splunkEvent);
        } catch (Exception e) {
            System.err.println("Failed to send business event to Splunk: " + e.getMessage());
        }
    }

    /**
     * Send error/exception event to Splunk
     */
    @Async
    public void logError(String operation, Exception exception, String userId, 
                        Map<String, Object> context) {
        try {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("operation", operation);
            eventData.put("error_type", exception.getClass().getSimpleName());
            eventData.put("error_message", exception.getMessage());
            eventData.put("user_id", userId);
            eventData.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            if (context != null) {
                eventData.put("context", context);
            }

            // Include stack trace for critical errors
            if (exception instanceof SecurityException || 
                exception instanceof IllegalStateException) {
                eventData.put("stack_trace", getStackTrace(exception));
            }

            Map<String, Object> splunkEvent = createSplunkEvent("error_event", eventData);
            sendToSplunk(splunkEvent);
        } catch (Exception e) {
            System.err.println("Failed to send error event to Splunk: " + e.getMessage());
        }
    }

    /**
     * Create standardized Splunk event structure
     */
    private Map<String, Object> createSplunkEvent(String eventType, Map<String, Object> eventData) {
        Map<String, Object> splunkEvent = new HashMap<>();
        
        // Splunk HEC format
        splunkEvent.put("time", System.currentTimeMillis() / 1000); // Unix timestamp
        splunkEvent.put("index", splunkIndex);
        splunkEvent.put("sourcetype", "json");
        splunkEvent.put("source", serviceName);
        
        // Event payload
        Map<String, Object> event = new HashMap<>();
        event.put("service", serviceName);
        event.put("event_type", eventType);
        event.put("event_id", UUID.randomUUID().toString());
        event.put("environment", getEnvironment());
        event.putAll(eventData);
        
        splunkEvent.put("event", event);
        
        return splunkEvent;
    }

    /**
     * Create security event payload for Splunk
     */
    private Map<String, Object> createSecurityEventPayload(SecurityEvent event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("user_id", event.getUserId());
        payload.put("event_type", event.getEventType().name());
        payload.put("risk_level", event.getRiskLevel().name());
        payload.put("successful", event.getSuccessful());
        payload.put("description", event.getDescription());
        payload.put("session_id", event.getSessionId());
        payload.put("ip_address", event.getIpAddress());
        payload.put("user_agent", event.getUserAgent());
        payload.put("location", event.getLocation());
        payload.put("device_fingerprint", event.getDeviceFingerprint());
        payload.put("failure_reason", event.getFailureReason());
        payload.put("correlation_id", event.getCorrelationId());
        payload.put("timestamp", event.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        if (event.getMetadata() != null) {
            payload.put("metadata", event.getMetadata());
        }
        
        return payload;
    }

    /**
     * Send event to Splunk HEC
     */
    private void sendToSplunk(Map<String, Object> splunkEvent) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Splunk " + splunkHecToken);

            String jsonPayload = objectMapper.writeValueAsString(splunkEvent);
            HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);

            restTemplate.exchange(splunkHecUrl, HttpMethod.POST, request, String.class);
        } catch (Exception e) {
            // Fallback to local logging if Splunk is unavailable
            System.err.println("Failed to send to Splunk: " + e.getMessage());
            System.out.println("Event would have been sent to Splunk: " + splunkEvent);
        }
    }

    /**
     * Get current environment (dev, staging, prod)
     */
    private String getEnvironment() {
        return System.getProperty("spring.profiles.active", "development");
    }

    /**
     * Get stack trace as string
     */
    private String getStackTrace(Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }
}
