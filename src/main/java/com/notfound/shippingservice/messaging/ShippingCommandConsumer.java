package com.notfound.shippingservice.messaging;

import com.notfound.shippingservice.messaging.dto.SagaMessageEnvelope;
import com.notfound.shippingservice.service.ShippingSagaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "bookstore.saga.enabled", havingValue = "true", matchIfMissing = true)
public class ShippingCommandConsumer {

    private final ShippingSagaService shippingSagaService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "${bookstore.saga.commands-queue:shipping.commands.queue}")
    public void onCommand(Message message) {
        SagaMessageEnvelope command;
        try {
            command = objectMapper.readValue(message.getBody(), SagaMessageEnvelope.class);
        } catch (Exception e) {
            log.error(
                    "Cannot deserialize shipping command routingKey={} body={}: {}",
                    message.getMessageProperties().getReceivedRoutingKey(),
                    new String(message.getBody(), StandardCharsets.UTF_8),
                    e.getMessage());
            return;
        }
        if (command == null || command.getType() == null) {
            log.warn("Received command without type");
            return;
        }
        log.info(
                "Received shipping command type={} sagaId={} eventId={}",
                command.getType(),
                command.getSagaId(),
                command.getEventId());
        switch (command.getType()) {
            case SagaMessageTypes.CREATE_COMMAND -> shippingSagaService.handleCreateCommand(command);
            case SagaMessageTypes.CANCEL_COMMAND -> shippingSagaService.handleCancelCommand(command);
            default -> log.warn("Unsupported command type={}", command.getType());
        }
    }
}
