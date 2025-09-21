package com.fintech.userservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintech.userservice.dto.message.AccountCreationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class AccountCreationKafkaPublisher {

    private static final Logger logger = LoggerFactory.getLogger(AccountCreationKafkaPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.topics.account-creation}")
    private String accountCreationTopic;

    final ObjectMapper objectMapper;

    public AccountCreationKafkaPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Publish user role registration message to Kafka for authorization service
     */
    public void publishUserRoleRegistration(AccountCreationMessage accountCreationMessage) {
        try {
            // Convert object to JSON string
            String jsonMessage = objectMapper.writeValueAsString(accountCreationMessage);

            kafkaTemplate.send(accountCreationTopic, accountCreationMessage.getUserId(), jsonMessage)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            logger.info("Published user account creation message to topic: {} with offset: {} for userId: {} with account number: {} and initial balance: {}",
                                    accountCreationTopic, result.getRecordMetadata().offset(),
                                    accountCreationMessage.getUserId(), accountCreationMessage.getAccountNumber(), accountCreationMessage.getInitialDeposit());
                        } else {
                            logger.error("Failed to publish user account creation message to topic: {} for userId: {} with account number: {} and initial balance: {}",
                                    accountCreationTopic, accountCreationMessage.getUserId(), accountCreationMessage.getAccountNumber(), accountCreationMessage.getInitialDeposit(), ex);
                            throw new RuntimeException("Failed to publish user role registration message", ex);
                        }
                    });

        } catch (Exception e) {
            logger.error("Failed to publish user account creation message to topic: {} for userId: {} with account number: {} and initial balance: {}",
                    accountCreationTopic, accountCreationMessage.getUserId(), accountCreationMessage.getAccountNumber(), accountCreationMessage.getInitialDeposit(), e);
            throw new RuntimeException("Failed to publish user role registration message", e);
        }
    }
}
