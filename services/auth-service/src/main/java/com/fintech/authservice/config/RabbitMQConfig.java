package com.fintech.authservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String EMAIL_QUEUE = "notification.email.queue";
    public static final String EMAIL_ROUTING_KEY = "notification.email";

    // User service messaging constants
    @Value("${rabbitmq.exchange.user}")
    private String USER_EXCHANGE;

    @Value("${rabbitmq.queue.user-creation}")
    private String USER_CREATION_QUEUE;

    @Value("${rabbitmq.routing-key.user-creation}")
    private String USER_CREATION_ROUTING_KEY;

    @Bean
    public Exchange notificationExchange() {
        return ExchangeBuilder.topicExchange(NOTIFICATION_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE).build();
    }

    @Bean
    public Binding emailBinding() {
        return BindingBuilder.bind(emailQueue())
                .to(notificationExchange())
                .with(EMAIL_ROUTING_KEY)
                .noargs();
    }

    // User service beans
    @Bean
    public Exchange userExchange() {
        return ExchangeBuilder.directExchange(USER_EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue userCreationQueue() {
        return QueueBuilder.durable(USER_CREATION_QUEUE).build();
    }

    @Bean
    public Binding userCreationBinding() {
        return BindingBuilder.bind(userCreationQueue())
                .to(userExchange())
                .with(USER_CREATION_ROUTING_KEY)
                .noargs();
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        return factory;
    }
}
