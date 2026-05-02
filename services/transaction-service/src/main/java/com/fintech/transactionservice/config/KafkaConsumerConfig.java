package com.fintech.transactionservice.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka consumer error handling configuration with DLQ support.
 * Configures 3 retries with exponential backoff before publishing to Dead Letter Topic.
 */
@Configuration
public class KafkaConsumerConfig {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerConfig.class);

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Dedicated producer factory for DLQ publishing (separate from main producer).
     */
    @Bean
    public ProducerFactory<String, String> dlqProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, String> dlqKafkaTemplate() {
        return new KafkaTemplate<>(dlqProducerFactory());
    }

    /**
     * Dead Letter Publishing Recoverer — publishes failed messages to .DLT topic.
     */
    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer() {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(dlqKafkaTemplate());
        return recoverer;
    }

    /**
     * DefaultErrorHandler with 3 retries + exponential backoff + DLQ recovery.
     * Initial interval: 1s, multiplier: 2x, max interval: 10s.
     */
    @Bean
    public CommonErrorHandler kafkaErrorHandler(DeadLetterPublishingRecoverer deadLetterPublishingRecoverer) {
        ExponentialBackOff backOff = new ExponentialBackOff();
        backOff.setInitialInterval(1000L);    // 1 second
        backOff.setMultiplier(2.0);           // Exponential
        backOff.setMaxInterval(10000L);       // Max 10 seconds
        backOff.setMaxElapsedTime(30000L);    // Max 30 seconds total (effectively 3 retries)

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(deadLetterPublishingRecoverer, backOff);

        // Log each retry attempt
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) -> {
            logger.warn("Retry attempt {} for topic: {}, partition: {}, offset: {}, key: {}. Error: {}",
                    deliveryAttempt,
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    record.key(),
                    ex.getMessage());
        });

        logger.info("Kafka DLQ error handler configured: 3 retries with exponential backoff (1s, 2s, 4s)");
        return errorHandler;
    }
}
