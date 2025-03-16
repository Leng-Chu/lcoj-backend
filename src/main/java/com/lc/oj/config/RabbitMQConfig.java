package com.lc.oj.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public Queue judgeQueue() {
        return new Queue("judgeQueue", true);
    }

    @Bean
    public DirectExchange judgeExchange() {
        return new DirectExchange("judgeExchange", true, false);
    }

    @Bean
    public Binding binding(Queue judgeQueue, DirectExchange judgeExchange) {
        return BindingBuilder.bind(judgeQueue).to(judgeExchange).with("judge");
    }
}