package com.fintech.retryservice.listener;

import com.fintech.retryservice.dto.RetryRequest;
import com.fintech.retryservice.model.RetryType;
import com.fintech.retryservice.service.RetryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kafka listener for incoming retry requests and callback results.
 */
@Component
public class RetryKafkaListener {

    private static final Logger logger = LoggerFactory.getLogger(RetryKafkaListener.class);

    private final RetryService retryService;

    public RetryKafkaListener(RetryService retryService) {
        this.retryService = retryService;
    }

    /**
     * Listen for new retry requests on the retry-requests topic.
     * Expected payload: { originalId, retryType, serviceName, endpointUrl, createdBy, maxRetries, retryDelaySeconds, retryData, priority }
     */
    @KafkaListener(topics = "${retry.kafka.topic.requests:retry-requests}", groupId = "${spring.kafka.consumer.group-id:retry-service}")
    public void handleRetryRequest(@Payload Map<String, Object> message, Acknowledgment ack) {
        try {
            logger.info("Received retry request via Kafka: originalId={}", message.get("originalId"));

            RetryRequest request = new RetryRequest();
            request.setOriginalId((String) message.get("originalId"));
            request.setServiceName((String) message.get("serviceName"));
            request.setEndpointUrl((String) message.get("endpointUrl"));
            request.setCreatedBy((String) message.getOrDefault("createdBy", "kafka-listener"));

            // Parse retry type
            String retryTypeStr = (String) message.get("retryType");
            if (retryTypeStr != null) {
                request.setRetryType(RetryType.valueOf(retryTypeStr));
            } else {
                request.setRetryType(RetryType.EXTERNAL_API_CALL);
            }

            // Optional fields
            if (message.get("maxRetries") != null) {
                request.setMaxRetries(((Number) message.get("maxRetries")).intValue());
            }
            if (message.get("retryDelaySeconds") != null) {
                request.setRetryDelaySeconds(((Number) message.get("retryDelaySeconds")).intValue());
            }
            if (message.get("priority") != null) {
                request.setPriority((String) message.get("priority"));
            }
            if (message.get("retryData") instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, String> data = (Map<String, String>) message.get("retryData");
                request.setRetryData(data);
            }

            retryService.scheduleRetry(request);
            ack.acknowledge();
            logger.info("Retry request processed and acknowledged: originalId={}", message.get("originalId"));

        } catch (Exception e) {
            logger.error("Error processing retry request from Kafka: {}", e.getMessage(), e);
            // Acknowledge to avoid reprocessing poison messages; error is logged
            ack.acknowledge();
        }
    }

    /**
     * Listen for retry callbacks (results from downstream services) on retry-callbacks topic.
     * Expected payload: { retryId, success, errorMessage, errorCode }
     */
    @KafkaListener(topics = "${retry.kafka.topic.callbacks:retry-callbacks}", groupId = "${spring.kafka.consumer.group-id:retry-service}")
    public void handleRetryCallback(@Payload Map<String, Object> message, Acknowledgment ack) {
        try {
            String retryId = (String) message.get("retryId");
            Boolean success = (Boolean) message.getOrDefault("success", false);
            String errorMessage = (String) message.get("errorMessage");

            logger.info("Received retry callback via Kafka: retryId={}, success={}", retryId, success);

            if (retryId == null || retryId.isBlank()) {
                logger.warn("Retry callback missing retryId, skipping");
                ack.acknowledge();
                return;
            }

            retryService.handleResult(retryId, success, errorMessage);
            ack.acknowledge();
            logger.info("Retry callback processed: retryId={}, success={}", retryId, success);

        } catch (Exception e) {
            logger.error("Error processing retry callback from Kafka: {}", e.getMessage(), e);
            ack.acknowledge();
        }
    }
}
