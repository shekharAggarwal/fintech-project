package com.fintech.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executor;

/**
 * Configuration for Splunk integration
 * Enables async processing and HTTP client setup
 */
@Configuration
@EnableAsync
public class SplunkConfig {

    /**
     * RestTemplate for Splunk HTTP Event Collector
     */
    @Bean
    public RestTemplate splunkRestTemplate() {
        return new RestTemplate();
    }

    /**
     * Async executor for Splunk logging
     * Prevents blocking main application threads
     */
    @Bean(name = "splunkAsyncExecutor")
    public Executor splunkAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(25);
        executor.setThreadNamePrefix("splunk-async-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
