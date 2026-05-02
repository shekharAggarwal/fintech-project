package com.fintech.notificationservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ─── Exchanges ──────────────────────────────────────────────────────────────
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String DLQ_EXCHANGE = "notification.dlx.exchange";

    // ─── Email Queue ────────────────────────────────────────────────────────────
    public static final String EMAIL_QUEUE = "notification.email";
    public static final String EMAIL_ROUTING_KEY = "notification.email";

    // ─── SMS Queue ──────────────────────────────────────────────────────────────
    public static final String SMS_QUEUE = "notification.sms";
    public static final String SMS_ROUTING_KEY = "notification.sms";

    // ─── Push Queue ─────────────────────────────────────────────────────────────
    public static final String PUSH_QUEUE = "notification.push";
    public static final String PUSH_ROUTING_KEY = "notification.push";

    // ─── Dead Letter Queues ─────────────────────────────────────────────────────
    public static final String DLQ_EMAIL = "notification.email.dlq";
    public static final String DLQ_SMS = "notification.sms.dlq";
    public static final String DLQ_PUSH = "notification.push.dlq";

    // ─── Exchange Beans ─────────────────────────────────────────────────────────

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLQ_EXCHANGE);
    }

    // ─── Main Queues (with DLX arguments) ───────────────────────────────────────

    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_EMAIL)
                .build();
    }

    @Bean
    public Queue smsQueue() {
        return QueueBuilder.durable(SMS_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_SMS)
                .build();
    }

    @Bean
    public Queue pushQueue() {
        return QueueBuilder.durable(PUSH_QUEUE)
                .withArgument("x-dead-letter-exchange", DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_PUSH)
                .build();
    }

    // ─── Dead Letter Queues ─────────────────────────────────────────────────────

    @Bean
    public Queue emailDlq() {
        return QueueBuilder.durable(DLQ_EMAIL).build();
    }

    @Bean
    public Queue smsDlq() {
        return QueueBuilder.durable(DLQ_SMS).build();
    }

    @Bean
    public Queue pushDlq() {
        return QueueBuilder.durable(DLQ_PUSH).build();
    }

    // ─── Bindings: Main Queues → Notification Exchange ──────────────────────────

    @Bean
    public Binding emailBinding() {
        return BindingBuilder
                .bind(emailQueue())
                .to(notificationExchange())
                .with(EMAIL_ROUTING_KEY);
    }

    @Bean
    public Binding smsBinding() {
        return BindingBuilder
                .bind(smsQueue())
                .to(notificationExchange())
                .with(SMS_ROUTING_KEY);
    }

    @Bean
    public Binding pushBinding() {
        return BindingBuilder
                .bind(pushQueue())
                .to(notificationExchange())
                .with(PUSH_ROUTING_KEY);
    }

    // ─── Bindings: DLQ Queues → DLX Exchange ────────────────────────────────────

    @Bean
    public Binding emailDlqBinding() {
        return BindingBuilder
                .bind(emailDlq())
                .to(deadLetterExchange())
                .with(DLQ_EMAIL);
    }

    @Bean
    public Binding smsDlqBinding() {
        return BindingBuilder
                .bind(smsDlq())
                .to(deadLetterExchange())
                .with(DLQ_SMS);
    }

    @Bean
    public Binding pushDlqBinding() {
        return BindingBuilder
                .bind(pushDlq())
                .to(deadLetterExchange())
                .with(DLQ_PUSH);
    }

    // ─── Message Converter & Container Factory ──────────────────────────────────

    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jackson2JsonMessageConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jackson2JsonMessageConverter());
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
