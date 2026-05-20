package com.notfound.shippingservice.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notfound.shippingservice.config.SagaShippingProperties;
import com.notfound.shippingservice.messaging.dto.SagaMessageEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "bookstore.saga.enabled", havingValue = "true", matchIfMissing = true)
public class SagaEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final SagaShippingProperties properties;
    private final ObjectMapper objectMapper;

    public void publishResult(SagaMessageEnvelope command, String eventType, String routingKey, Object payload) {
        SagaMessageEnvelope event = new SagaMessageEnvelope();
        event.setEventId(UUID.randomUUID());
        event.setSagaId(command.getSagaId());
        event.setCorrelationId(command.getCorrelationId() != null ? command.getCorrelationId() : command.getSagaId());
        event.setCausationId(command.getEventId());
        event.setType(eventType);
        event.setOccurredAt(Instant.now());
        event.setOrderId(command.getOrderId());
        event.setUserId(command.getUserId());
        event.setPayload(objectMapper.valueToTree(payload));

        rabbitTemplate.convertAndSend(properties.getEventsExchange(), routingKey, event);
        log.info("Published saga event type={} sagaId={} orderId={}", eventType, event.getSagaId(), event.getOrderId());
    }
}
