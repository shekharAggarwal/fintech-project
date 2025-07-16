package com.fintech.notificationservice.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String EMAIL_QUEUE = "notification.email";
    public static final String EMAIL_ROUTING_KEY = "notification.email";
    
    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE);
    }
    
    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE).build();
    }
    
    @Bean
    public Binding emailBinding() {
        return BindingBuilder
                .bind(emailQueue())
                .to(notificationExchange())
                .with(EMAIL_ROUTING_KEY);
    }
}
