package com.fintech.authorizationservice.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class AdminEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(AdminEventPublisher.class);
    private static final String TOPIC = "authz-admin-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public AdminEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public void publishRoleCreated(Long roleId, String roleName) {
        publishEvent("ROLE_CREATED", Map.of("roleId", roleId, "roleName", roleName));
    }

    public void publishRoleUpdated(Long roleId, String roleName) {
        publishEvent("ROLE_UPDATED", Map.of("roleId", roleId, "roleName", roleName));
    }

    public void publishRoleDeleted(Long roleId, String roleName) {
        publishEvent("ROLE_DELETED", Map.of("roleId", roleId, "roleName", roleName));
    }

    public void publishPermissionChanged(Long roleId, Long permissionId, String action) {
        publishEvent("PERMISSION_CHANGED", Map.of("roleId", roleId, "permissionId", permissionId, "action", action));
    }

    private void publishEvent(String eventType, Map<String, Object> payload) {
        try {
            Map<String, Object> event = new HashMap<>(payload);
            event.put("eventType", eventType);
            event.put("timestamp", Instant.now().toString());
            event.put("source", "authorization-service");

            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, eventType, message)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish event {}: {}", eventType, ex.getMessage());
                        } else {
                            log.debug("Published event {}: {}", eventType, message);
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event {}: {}", eventType, e.getMessage());
        }
    }
}
