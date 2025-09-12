package com.fintech.userservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

@Component
public class AuthorizationServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(AuthorizationServiceClient.class);

    private final WebClient webClient;


    public AuthorizationServiceClient(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Update user role in authorization service synchronously
     * This call will fail the entire operation if authorization service is unreachable
     */
    public void updateUserRole(String userId, String newRole, String updatedBy) {
        logger.info("Calling authorization service to update role for userId: {} to role: {}", userId, newRole);

        Map<String, String> request = Map.of(
                "userId", userId,
                "newRole", newRole,
                "updatedBy", updatedBy,
                "serviceSource", "user-service"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Service-Name", "user-service"); // For service identification

        HttpEntity<Map<String, String>> httpEntity = new HttpEntity<>(request, headers);

        try {
            // Make synchronous call to authorization service
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> responseEntity = webClient.put()
                    .uri("/api/authz/internal/user-role")
                    .bodyValue(httpEntity)
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {
                    }) // Type-safe way for generics
                    .block(); // This makes the call blocking

            // 3. Check for a null response and log the details from the ResponseEntity
            if (responseEntity != null) {
                logger.info("Successfully updated user role in authorization service for userId: {} to role: {} - Status: {}, Response: {}",
                        userId,
                        newRole,
                        responseEntity.getStatusCode(),
                        responseEntity.getBody());
            } else {
                logger.warn("No response received from authorization service for userId: {}", userId);
            }

        } catch (WebClientResponseException e) {
            // It's crucial to catch exceptions for non-2xx responses
            logger.error("Error updating user role for userId: {}. Status: {}, Body: {}",
                    userId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("Failed to communicate with authorization service: " + e.getMessage(), e);
        }
    }
}
