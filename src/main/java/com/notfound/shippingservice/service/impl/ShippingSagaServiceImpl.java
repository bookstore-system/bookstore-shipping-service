package com.notfound.shippingservice.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notfound.shippingservice.config.SagaShippingProperties;
import com.notfound.shippingservice.exception.ShippingServiceException;
import com.notfound.shippingservice.messaging.SagaEventPublisher;
import com.notfound.shippingservice.messaging.SagaMessageTypes;
import com.notfound.shippingservice.messaging.dto.SagaMessageEnvelope;
import com.notfound.shippingservice.messaging.dto.ShippingCancelPayload;
import com.notfound.shippingservice.messaging.dto.ShippingCreatePayload;
import com.notfound.shippingservice.model.dto.request.CancelShippingOrderRequest;
import com.notfound.shippingservice.model.dto.request.CreateShippingOrderRequest;
import com.notfound.shippingservice.model.dto.response.CancelShippingOrderResponse;
import com.notfound.shippingservice.model.dto.response.CreateShippingOrderResponse;
import com.notfound.shippingservice.model.entity.ProcessedMessage;
import com.notfound.shippingservice.model.entity.Shipment;
import com.notfound.shippingservice.model.entity.Shipment.Status;
import com.notfound.shippingservice.repository.ProcessedMessageRepository;
import com.notfound.shippingservice.repository.ShipmentRepository;
import com.notfound.shippingservice.service.ShippingSagaService;
import com.notfound.shippingservice.service.ShippingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "bookstore.saga.enabled", havingValue = "true", matchIfMissing = true)
public class ShippingSagaServiceImpl implements ShippingSagaService {

    private final ShippingService shippingService;
    private final ShipmentRepository shipmentRepository;
    private final ProcessedMessageRepository processedMessageRepository;
    private final SagaEventPublisher sagaEventPublisher;
    private final SagaShippingProperties sagaShippingProperties;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void handleCreateCommand(SagaMessageEnvelope command) {
        if (!beginIdempotent(command)) {
            return;
        }

        Optional<Shipment> existing = shipmentRepository.findBySagaId(command.getSagaId());
        if (existing.isPresent() && existing.get().getStatus() == Status.CREATED) {
            publishCreated(command, existing.get());
            return;
        }

        ShippingCreatePayload payload;
        try {
            payload = objectMapper.convertValue(command.getPayload(), ShippingCreatePayload.class);
        } catch (Exception ex) {
            failCreate(command, "Invalid payload: " + ex.getMessage());
            return;
        }

        CreateShippingOrderRequest request = toCreateRequest(payload);
        CreateShippingOrderResponse response = createWithRetry(command, request);
        if (response == null) {
            return;
        }

        Shipment shipment = existing.orElseGet(() -> Shipment.builder()
                .sagaId(command.getSagaId())
                .orderId(command.getOrderId())
                .build());
        shipment.setOrderId(command.getOrderId());
        shipment.setShippingOrderCode(response.getOrderCode());
        shipment.setCodAmount(payload.getCodAmount());
        shipment.setTotalFee(response.getTotalFee());
        shipment.setExpectedDeliveryTime(response.getExpectedDeliveryTime());
        shipment.setStatus(Status.CREATED);
        shipment.setLastError(null);
        shipmentRepository.save(shipment);

        publishCreated(command, shipment);
    }

    @Override
    @Transactional
    public void handleCancelCommand(SagaMessageEnvelope command) {
        if (!beginIdempotent(command)) {
            return;
        }

        Optional<Shipment> existing = shipmentRepository.findBySagaId(command.getSagaId());
        if (existing.isEmpty()) {
            publishCancelled(command, null);
            return;
        }

        Shipment shipment = existing.get();
        if (shipment.getStatus() == Status.CANCELLED) {
            publishCancelled(command, shipment);
            return;
        }

        String orderCode = resolveCancelOrderCode(command, shipment);
        if (orderCode == null) {
            failCancel(command, "Missing shippingOrderCode for saga " + command.getSagaId());
            return;
        }

        try {
            CancelShippingOrderRequest request = new CancelShippingOrderRequest();
            request.setOrderCode(orderCode);
            CancelShippingOrderResponse response = shippingService.cancelOrder(request);
            if (response.getCancelled() == null || !response.getCancelled()) {
                throw new ShippingServiceException("GHN rejected cancel: " + response.getMessage());
            }
            shipment.setStatus(Status.CANCELLED);
            shipment.setLastError(null);
            shipmentRepository.save(shipment);
            publishCancelled(command, shipment);
        } catch (Exception ex) {
            log.error("Cancel shipment failed sagaId={} orderCode={}", command.getSagaId(), orderCode, ex);
            shipment.setLastError(ex.getMessage());
            shipmentRepository.save(shipment);
            failCancel(command, ex.getMessage());
        }
    }

    private CreateShippingOrderResponse createWithRetry(SagaMessageEnvelope command, CreateShippingOrderRequest request) {
        int maxAttempts = Math.max(1, sagaShippingProperties.getCreateRetryMax() + 1);
        long delay = Math.max(0, sagaShippingProperties.getCreateRetryDelayMs());
        Exception lastError = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return shippingService.createOrder(request);
            } catch (Exception ex) {
                lastError = ex;
                log.warn(
                        "GHN create order attempt {}/{} failed sagaId={}: {}",
                        attempt,
                        maxAttempts,
                        command.getSagaId(),
                        ex.getMessage());
                if (attempt < maxAttempts && delay > 0) {
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        failCreate(command, lastError != null ? lastError.getMessage() : "Unknown GHN error");
        return null;
    }

    private String resolveCancelOrderCode(SagaMessageEnvelope command, Shipment shipment) {
        if (shipment.getShippingOrderCode() != null) {
            return shipment.getShippingOrderCode();
        }
        if (command.getPayload() == null) {
            return null;
        }
        try {
            ShippingCancelPayload cancelPayload = objectMapper.convertValue(command.getPayload(), ShippingCancelPayload.class);
            return cancelPayload.getShippingOrderCode();
        } catch (Exception ex) {
            log.warn("Failed to parse cancel payload sagaId={}: {}", command.getSagaId(), ex.getMessage());
            return null;
        }
    }

    private boolean beginIdempotent(SagaMessageEnvelope command) {
        if (command.getEventId() == null || command.getSagaId() == null) {
            log.warn("Ignoring command without eventId or sagaId type={}", command.getType());
            return false;
        }
        if (processedMessageRepository.existsById(command.getEventId())) {
            log.info("Skipping duplicate command eventId={} type={}", command.getEventId(), command.getType());
            return false;
        }
        processedMessageRepository.save(ProcessedMessage.builder()
                .messageId(command.getEventId())
                .messageType(command.getType())
                .build());
        return true;
    }

    private CreateShippingOrderRequest toCreateRequest(ShippingCreatePayload payload) {
        CreateShippingOrderRequest request = new CreateShippingOrderRequest();
        request.setToName(payload.getToName());
        request.setToPhone(payload.getToPhone());
        request.setToAddress(payload.getToAddress());
        request.setToWardName(payload.getToWardName());
        request.setToDistrictName(payload.getToDistrictName());
        request.setToProvinceName(payload.getToProvinceName());
        request.setCodAmount(payload.getCodAmount() == null ? 0 : payload.getCodAmount());
        if (payload.getNote() != null) {
            request.setNote(payload.getNote());
        }
        if (payload.getContent() != null) {
            request.setContent(payload.getContent());
        }
        if (payload.getLength() != null) {
            request.setLength(payload.getLength());
        }
        if (payload.getWidth() != null) {
            request.setWidth(payload.getWidth());
        }
        if (payload.getHeight() != null) {
            request.setHeight(payload.getHeight());
        }
        if (payload.getWeight() != null) {
            request.setWeight(payload.getWeight());
        }
        if (payload.getInsuranceValue() != null) {
            request.setInsuranceValue(payload.getInsuranceValue());
        }
        request.setClientOrderCode(payload.getClientOrderCode());
        return request;
    }

    private void publishCreated(SagaMessageEnvelope command, Shipment shipment) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("shippingOrderCode", shipment.getShippingOrderCode());
        payload.put("totalFee", shipment.getTotalFee());
        payload.put("expectedDeliveryTime", shipment.getExpectedDeliveryTime());
        payload.put("codAmount", shipment.getCodAmount());
        sagaEventPublisher.publishResult(
                command, SagaMessageTypes.CREATED_EVENT, SagaMessageTypes.RK_CREATED_EVENT, payload);
    }

    private void publishCancelled(SagaMessageEnvelope command, Shipment shipment) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("shippingOrderCode", shipment != null ? shipment.getShippingOrderCode() : null);
        sagaEventPublisher.publishResult(
                command, SagaMessageTypes.CANCELLED_EVENT, SagaMessageTypes.RK_CANCELLED_EVENT, payload);
    }

    private void failCreate(SagaMessageEnvelope command, String reason) {
        Shipment shipment = shipmentRepository.findBySagaId(command.getSagaId())
                .orElseGet(() -> Shipment.builder()
                        .sagaId(command.getSagaId())
                        .orderId(command.getOrderId())
                        .build());
        shipment.setOrderId(command.getOrderId());
        shipment.setStatus(Status.FAILED);
        shipment.setLastError(reason);
        shipmentRepository.save(shipment);
        sagaEventPublisher.publishResult(
                command,
                SagaMessageTypes.FAILED_EVENT,
                SagaMessageTypes.RK_FAILED_EVENT,
                Map.of("reason", reason, "step", "create"));
        log.warn("Shipping create failed sagaId={} reason={}", command.getSagaId(), reason);
    }

    private void failCancel(SagaMessageEnvelope command, String reason) {
        sagaEventPublisher.publishResult(
                command,
                SagaMessageTypes.FAILED_EVENT,
                SagaMessageTypes.RK_FAILED_EVENT,
                Map.of("reason", reason, "step", "cancel"));
        log.warn("Shipping cancel failed sagaId={} reason={}", command.getSagaId(), reason);
    }
}
