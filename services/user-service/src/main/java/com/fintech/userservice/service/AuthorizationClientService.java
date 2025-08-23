package com.fintech.userservice.service;

import com.fintech.userservice.dto.AuthorizationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Client service for communicating with Authorization Service
 * Handles real-time permission checking with cache invalidation
 */
@Service
public class AuthorizationClientService {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationClientService.class);

    private RestTemplate restTemplate;

    @Value("${authorization.service.url:http://authorization-service:8083}")
    private String authorizationServiceUrl;

    @Value("${authorization.service.timeout:5000}")
    private int timeoutMs;

    public AuthorizationClientService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Check permission with fresh data (bypasses stale cache)
     */
    public AuthorizationResponse checkPermissionWithFreshData(String token, String resource, String action) {
        try {
            logger.debug("Checking permission for resource: {}, action: {}", resource, action);

            // Create request body
            Map<String, String> request = new HashMap<>();
            request.put("token", token);
            request.put("resource", resource);
            request.put("action", action);
            request.put("freshCheck", "true"); // Request fresh permission check

            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");

            // Create request entity
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(request, headers);

            // Call authorization service
            ResponseEntity<AuthorizationResponse> responseEntity = restTemplate.exchange(
                    authorizationServiceUrl + "/api/v1/authorization/authorize",
                    HttpMethod.POST,
                    requestEntity,
                    AuthorizationResponse.class
            );

            AuthorizationResponse response = responseEntity.getBody();

            if (response != null) {
                logger.debug("Authorization response: authorized={}, reason={}",
                        response.isAuthorized(), response.getReason());
                return response;
            } else {
                logger.error("Null response from authorization service");
                return new AuthorizationResponse(false, "Authorization service returned null response");
            }

        } catch (HttpClientErrorException e) {
            logger.error("Authorization service error - Status: {}, Body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());

            if (e.getStatusCode().value() == 403) {
                return new AuthorizationResponse(false, "Access denied by authorization service");
            } else if (e.getStatusCode().value() == 401) {
                return new AuthorizationResponse(false, "Invalid or expired token");
            } else {
                return new AuthorizationResponse(false, "Authorization service error: " + e.getMessage());
            }

        } catch (Exception e) {
            logger.error("Failed to check authorization for resource: {}, action: {}", resource, action, e);

            // Fail secure - deny access on any error
            return new AuthorizationResponse(false, "Authorization check failed: " + e.getMessage());
        }
    }

    /**
     * Check basic permission (may use cached data)
     */
    public AuthorizationResponse checkPermission(String token, String resource, String action) {
        try {
            logger.debug("Checking permission (cached) for resource: {}, action: {}", resource, action);

            // Create request body
            Map<String, String> request = new HashMap<>();
            request.put("token", token);
            request.put("resource", resource);
            request.put("action", action);

            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");

            // Create request entity
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(request, headers);

            // Call authorization service
            ResponseEntity<AuthorizationResponse> responseEntity = restTemplate.exchange(
                    authorizationServiceUrl + "/api/v1/authorization/authorize",
                    HttpMethod.POST,
                    requestEntity,
                    AuthorizationResponse.class
            );

            AuthorizationResponse response = responseEntity.getBody();

            if (response != null) {
                return response;
            } else {
                return new AuthorizationResponse(false, "Authorization service returned null response");
            }

        } catch (Exception e) {
            logger.error("Failed to check authorization", e);
            return new AuthorizationResponse(false, "Authorization check failed");
        }
    }

    /**
     * Notify authorization service about role change
     * This will trigger cache invalidation and session updates
     */
    public void notifyRoleChange(String userEmail, String oldRole, String newRole, String changedBy) {
        try {
            logger.info("Notifying authorization service about role change for user: {} from {} to {}",
                    userEmail, oldRole, newRole);

            Map<String, String> notification = new HashMap<>();
            notification.put("userEmail", userEmail);
            notification.put("oldRole", oldRole);
            notification.put("newRole", newRole);
            notification.put("changedBy", changedBy);
            notification.put("timestamp", java.time.LocalDateTime.now().toString());

            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");

            // Create request entity
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(notification, headers);

            // Send notification to authorization service (async - don't wait for response)
            try {
                restTemplate.exchange(
                        authorizationServiceUrl + "/api/v1/authorization/role-change",
                        HttpMethod.POST,
                        requestEntity,
                        String.class
                );
                logger.info("Role change notification sent successfully for user: {}", userEmail);
            } catch (Exception notificationError) {
                logger.error("Failed to send role change notification for user: {}", userEmail, notificationError);
            }

        } catch (Exception e) {
            logger.error("Failed to prepare role change notification for user: {}", userEmail, e);
            // Don't throw exception - role change should succeed even if notification fails
        }
    }

    /**
     * Validate token and get user information
     */
    public AuthorizationResponse validateToken(String token) {
        try {
            logger.debug("Validating token");

            // Create headers with token
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);

            // Create request entity
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<AuthorizationResponse> responseEntity = restTemplate.exchange(
                    authorizationServiceUrl + "/api/v1/authorization/validate",
                    HttpMethod.GET,
                    requestEntity,
                    AuthorizationResponse.class
            );

            AuthorizationResponse response = responseEntity.getBody();
            return response != null ? response : new AuthorizationResponse(false, "Token validation failed");

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 401) {
                return new AuthorizationResponse(false, "Invalid or expired token");
            } else {
                return new AuthorizationResponse(false, "Token validation error");
            }

        } catch (Exception e) {
            logger.error("Failed to validate token", e);
            return new AuthorizationResponse(false, "Token validation failed");
        }
    }

    /**
     * Get user authorities/permissions
     */
    public String[] getUserAuthorities(String token) {
        try {
            logger.debug("Getting user authorities");

            // Create headers with token
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);

            // Create request entity
            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<String[]> responseEntity = restTemplate.exchange(
                    authorizationServiceUrl + "/api/v1/authorization/authorities",
                    HttpMethod.GET,
                    requestEntity,
                    String[].class
            );

            String[] authorities = responseEntity.getBody();
            return authorities != null ? authorities : new String[0];

        } catch (Exception e) {
            logger.error("Failed to get user authorities", e);
            return new String[0];
        }
    }

    /**
     * Health check for authorization service
     */
    public boolean isAuthorizationServiceHealthy() {
        try {
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    authorizationServiceUrl + "/api/v1/authorization/health",
                    HttpMethod.GET,
                    null,
                    String.class
            );

            String response = responseEntity.getBody();
            return response != null && response.contains("healthy");

        } catch (Exception e) {
            logger.warn("Authorization service health check failed", e);
            return false;
        }
    }
}
