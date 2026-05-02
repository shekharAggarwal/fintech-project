package com.fintech.paymentservice.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Dead Letter Publishing Recoverer - publishes failed messages to DLQ topic
     * Topic naming convention: original-topic.DLT
     */
    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        logger.info("DeadLetterPublishingRecoverer configured");
        return recoverer;
    }

    /**
     * Default error handler with exponential backoff and DLQ recovery
     * - 3 retries with exponential backoff (1s, 2s, 4s)
     * - After exhausting retries, message is sent to DLT (Dead Letter Topic)
     */
    @Bean
    public DefaultErrorHandler defaultErrorHandler(DeadLetterPublishingRecoverer recoverer) {
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(1000L);   // 1 second initial
        backOff.setMultiplier(2.0);          // Double each time
        backOff.setMaxInterval(10000L);      // Max 10 seconds

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        // Add non-retryable exceptions
        errorHandler.addNotRetryableExceptions(
            IllegalArgumentException.class,
            com.fasterxml.jackson.core.JsonParseException.class
        );

        logger.info("DefaultErrorHandler configured with 3 retries and exponential backoff");
        return errorHandler;
    }
}
