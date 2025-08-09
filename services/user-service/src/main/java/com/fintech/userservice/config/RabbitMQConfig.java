package com.fintech.userservice.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    @Value("${rabbitmq.exchange.user}")
    private String userExchange;
    
    @Value("${rabbitmq.queue.user-creation}")
    private String userCreationQueue;
    
    @Value("${rabbitmq.routing-key.user-creation}")
    private String userCreationRoutingKey;
    
    @Bean
    public DirectExchange userExchange() {
        return new DirectExchange(userExchange);
    }
    
    @Bean
    public Queue userCreationQueue() {
        return new Queue(userCreationQueue, true); // durable queue
    }
    
    @Bean
    public Binding userCreationBinding() {
        return BindingBuilder
                .bind(userCreationQueue())
                .to(userExchange())
                .with(userCreationRoutingKey);
    }
    
    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
