package com.fintech.authorizationservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.authorizationservice.dto.request.SessionCreationMessage;
import com.fintech.authorizationservice.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
public class SessionKafkaListener {

    private static final Logger logger = LoggerFactory.getLogger(SessionKafkaListener.class);

    private final SessionService sessionService;
    private final ObjectMapper objectMapper;

    public SessionKafkaListener(SessionService sessionService, ObjectMapper objectMapper) {
        this.sessionService = sessionService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${kafka.topics.session-creation}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleSessionCreationMessage(
            @Payload String jsonMessage,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        try {
            SessionCreationMessage sessionCreationMessage = objectMapper.readValue(jsonMessage, SessionCreationMessage.class);

            logger.info("Received session creation message from topic: {}, partition: {}, offset: {} for sessionId: {}",
                    topic, partition, offset, sessionCreationMessage.getSessionId());

            // Create session using the service
            sessionService.createSession(
                    sessionCreationMessage.getSessionId(),
                    sessionCreationMessage.getUserId(),
                    sessionCreationMessage.getExpiryTime()
            );

            logger.info("Session created successfully in database: sessionId={}, userId={}",
                    sessionCreationMessage.getSessionId(), sessionCreationMessage.getUserId());

            // Manually acknowledge the message
            acknowledgment.acknowledge();

        } catch (Exception e) {
            logger.error("Failed to process session creation message for sessionId: {}, topic: {}, partition: {}, offset: {}",
                    jsonMessage, topic, partition, offset, e);

            // Don't acknowledge on error - this will cause the message to be retried
            throw new RuntimeException("Failed to process session creation message", e);
        }
    }
}
