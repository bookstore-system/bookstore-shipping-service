package com.notfound.shippingservice.config;

import com.notfound.shippingservice.messaging.SagaMessageTypes;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "bookstore.saga.enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMqConfig {

    @Bean
    TopicExchange commandsExchange(SagaShippingProperties properties) {
        return new TopicExchange(properties.getCommandsExchange(), true, false);
    }

    @Bean
    TopicExchange eventsExchange(SagaShippingProperties properties) {
        return new TopicExchange(properties.getEventsExchange(), true, false);
    }

    @Bean
    Queue shippingCommandsQueue(SagaShippingProperties properties) {
        return new Queue(properties.getCommandsQueue(), true);
    }

    @Bean
    Binding createCommandBinding(Queue shippingCommandsQueue, TopicExchange commandsExchange) {
        return BindingBuilder.bind(shippingCommandsQueue)
                .to(commandsExchange)
                .with(SagaMessageTypes.RK_CREATE_COMMAND);
    }

    @Bean
    Binding cancelCommandBinding(Queue shippingCommandsQueue, TopicExchange commandsExchange) {
        return BindingBuilder.bind(shippingCommandsQueue)
                .to(commandsExchange)
                .with(SagaMessageTypes.RK_CANCEL_COMMAND);
    }

    @Bean
    MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
