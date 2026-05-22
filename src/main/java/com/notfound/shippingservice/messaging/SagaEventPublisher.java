package com.notfound.shippingservice.messaging;

import com.notfound.shippingservice.config.SagaShippingProperties;
import com.notfound.shippingservice.messaging.dto.SagaMessageEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;
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
        event.setOccurredAt(LocalDateTime.now());
        event.setOrderId(command.getOrderId());
        event.setUserId(command.getUserId());
        event.setPayload(objectMapper.valueToTree(payload));
        copyShippingResultFields(event, payload);

        rabbitTemplate.convertAndSend(properties.getEventsExchange(), routingKey, event, this::removeJavaTypeHeaders);
        log.info("Published saga event type={} sagaId={} orderId={}", eventType, event.getSagaId(), event.getOrderId());
    }

    private void copyShippingResultFields(SagaMessageEnvelope event, Object payload) {
        if (!(payload instanceof Map<?, ?> map)) {
            return;
        }
        event.setShippingOrderCode(stringValue(map.get("shippingOrderCode")));
        event.setTotalFee(doubleValue(map.get("totalFee")));
        event.setExpectedDeliveryTime(stringValue(map.get("expectedDeliveryTime")));
        event.setCodAmount(integerValue(map.get("codAmount")));
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private Double doubleValue(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Integer integerValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Message removeJavaTypeHeaders(Message message) {
        message.getMessageProperties().getHeaders().remove("__TypeId__");
        message.getMessageProperties().getHeaders().remove("__ContentTypeId__");
        message.getMessageProperties().getHeaders().remove("__KeyTypeId__");
        return message;
    }
}
