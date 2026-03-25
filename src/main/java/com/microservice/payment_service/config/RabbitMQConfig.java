package com.microservice.payment_service.config;


import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // 🔁 Exchange
    public static final String EXCHANGE = "order.exchange";

    // 📥 Queue (Payment consumes Order events)
    public static final String PAYMENT_QUEUE = "payment.queue";


    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue paymentQueue() {
        return new Queue(PAYMENT_QUEUE, true);
    }

    @Bean
    public Binding orderCreatedBinding() {
        return BindingBuilder
                .bind(paymentQueue())
                .to(exchange())
                .with("order.created");
    }
}