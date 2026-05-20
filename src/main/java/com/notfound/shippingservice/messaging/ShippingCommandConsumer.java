package com.notfound.shippingservice.messaging;

import com.notfound.shippingservice.messaging.dto.SagaMessageEnvelope;
import com.notfound.shippingservice.service.ShippingSagaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "bookstore.saga.enabled", havingValue = "true", matchIfMissing = true)
public class ShippingCommandConsumer {

    private final ShippingSagaService shippingSagaService;

    @RabbitListener(queues = "${bookstore.saga.commands-queue:shipping.commands.queue}")
    public void onCommand(SagaMessageEnvelope command) {
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
