package com.notfound.shippingservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.notfound.shippingservice.config.SagaShippingProperties;
import com.notfound.shippingservice.exception.ShippingServiceException;
import com.notfound.shippingservice.messaging.SagaEventPublisher;
import com.notfound.shippingservice.messaging.dto.SagaMessageEnvelope;
import com.notfound.shippingservice.messaging.dto.ShippingCancelPayload;
import com.notfound.shippingservice.messaging.dto.ShippingCreatePayload;
import com.notfound.shippingservice.model.dto.response.CancelShippingOrderResponse;
import com.notfound.shippingservice.model.dto.response.CreateShippingOrderResponse;
import com.notfound.shippingservice.model.entity.Shipment;
import com.notfound.shippingservice.model.entity.Shipment.Status;
import com.notfound.shippingservice.repository.ProcessedMessageRepository;
import com.notfound.shippingservice.repository.ShipmentRepository;
import com.notfound.shippingservice.service.impl.ShippingSagaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShippingSagaServiceImplTest {

    @Mock
    ShippingService shippingService;

    @Mock
    ShipmentRepository shipmentRepository;

    @Mock
    ProcessedMessageRepository processedMessageRepository;

    @Mock
    SagaEventPublisher sagaEventPublisher;

    @Mock
    SagaShippingProperties sagaShippingProperties;

    ShippingSagaServiceImpl shippingSagaService;

    final ObjectMapper objectMapper = new ObjectMapper();

    UUID sagaId;
    UUID orderId;

    @BeforeEach
    void setUp() {
        shippingSagaService = new ShippingSagaServiceImpl(
                shippingService,
                shipmentRepository,
                processedMessageRepository,
                sagaEventPublisher,
                sagaShippingProperties,
                objectMapper);
        sagaId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        when(processedMessageRepository.existsById(any())).thenReturn(false);
    }

    @Test
    void create_publishesShippingCreatedAndStoresShipment() {
        SagaMessageEnvelope command = createCommand(0);
        when(shipmentRepository.findBySagaId(sagaId)).thenReturn(Optional.empty());
        when(sagaShippingProperties.getCreateRetryMax()).thenReturn(0);
        when(sagaShippingProperties.getCreateRetryDelayMs()).thenReturn(0L);
        when(shippingService.createOrder(any())).thenReturn(CreateShippingOrderResponse.builder()
                .orderCode("GHN_ABC")
                .totalFee(35000D)
                .build());
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(inv -> inv.getArgument(0));

        shippingSagaService.handleCreateCommand(command);

        ArgumentCaptor<Shipment> captor = ArgumentCaptor.forClass(Shipment.class);
        verify(shipmentRepository).save(captor.capture());
        Shipment saved = captor.getValue();
        assertThat(saved.getShippingOrderCode()).isEqualTo("GHN_ABC");
        assertThat(saved.getStatus()).isEqualTo(Status.CREATED);
        verify(sagaEventPublisher).publishResult(any(), eq("shipping.created"), eq("shipping.created"), any());
    }

    @Test
    void create_skipsWhenShipmentAlreadyCreatedForSaga() {
        SagaMessageEnvelope command = createCommand(0);
        Shipment existing = Shipment.builder()
                .sagaId(sagaId)
                .orderId(orderId)
                .shippingOrderCode("GHN_OLD")
                .status(Status.CREATED)
                .build();
        when(shipmentRepository.findBySagaId(sagaId)).thenReturn(Optional.of(existing));

        shippingSagaService.handleCreateCommand(command);

        verify(shippingService, never()).createOrder(any());
        verify(sagaEventPublisher).publishResult(any(), eq("shipping.created"), eq("shipping.created"), any());
    }

    @Test
    void create_retriesAndFailsWhenGhnKeepsFailing() {
        SagaMessageEnvelope command = createCommand(0);
        when(shipmentRepository.findBySagaId(sagaId)).thenReturn(Optional.empty());
        when(sagaShippingProperties.getCreateRetryMax()).thenReturn(1);
        when(sagaShippingProperties.getCreateRetryDelayMs()).thenReturn(0L);
        when(shippingService.createOrder(any())).thenThrow(new ShippingServiceException("GHN down"));

        shippingSagaService.handleCreateCommand(command);

        verify(shippingService, times(2)).createOrder(any());
        verify(sagaEventPublisher).publishResult(any(), eq("shipping.failed"), eq("shipping.failed"), any());
    }

    @Test
    void cancel_publishesCancelledWhenShipmentCreated() {
        SagaMessageEnvelope command = cancelCommand(null);
        Shipment shipment = Shipment.builder()
                .sagaId(sagaId)
                .orderId(orderId)
                .shippingOrderCode("GHN_ABC")
                .status(Status.CREATED)
                .build();
        when(shipmentRepository.findBySagaId(sagaId)).thenReturn(Optional.of(shipment));
        when(shippingService.cancelOrder(any())).thenReturn(CancelShippingOrderResponse.builder()
                .orderCode("GHN_ABC")
                .cancelled(true)
                .message("OK")
                .build());

        shippingSagaService.handleCancelCommand(command);

        verify(sagaEventPublisher).publishResult(any(), eq("shipping.cancelled"), eq("shipping.cancelled"), any());
        assertThat(shipment.getStatus()).isEqualTo(Status.CANCELLED);
    }

    @Test
    void cancel_isIdempotentForAlreadyCancelledShipment() {
        SagaMessageEnvelope command = cancelCommand(null);
        Shipment shipment = Shipment.builder()
                .sagaId(sagaId)
                .orderId(orderId)
                .shippingOrderCode("GHN_ABC")
                .status(Status.CANCELLED)
                .build();
        when(shipmentRepository.findBySagaId(sagaId)).thenReturn(Optional.of(shipment));

        shippingSagaService.handleCancelCommand(command);

        verify(shippingService, never()).cancelOrder(any());
        verify(sagaEventPublisher).publishResult(any(), eq("shipping.cancelled"), eq("shipping.cancelled"), any());
    }

    private SagaMessageEnvelope createCommand(int codAmount) {
        SagaMessageEnvelope envelope = baseCommand("shipping.create.command");
        ShippingCreatePayload payload = new ShippingCreatePayload();
        payload.setToName("Tester");
        payload.setToPhone("0900000000");
        payload.setToAddress("Test Address");
        payload.setToWardName("Phường 1");
        payload.setToDistrictName("Quận 1");
        payload.setToProvinceName("Hồ Chí Minh");
        payload.setCodAmount(codAmount);
        envelope.setPayload(objectMapper.valueToTree(payload));
        return envelope;
    }

    private SagaMessageEnvelope cancelCommand(String shippingOrderCode) {
        SagaMessageEnvelope envelope = baseCommand("shipping.cancel.command");
        ShippingCancelPayload payload = new ShippingCancelPayload();
        payload.setShippingOrderCode(shippingOrderCode);
        envelope.setPayload(objectMapper.valueToTree(payload));
        return envelope;
    }

    private SagaMessageEnvelope baseCommand(String type) {
        SagaMessageEnvelope envelope = new SagaMessageEnvelope();
        envelope.setEventId(UUID.randomUUID());
        envelope.setSagaId(sagaId);
        envelope.setOrderId(orderId);
        envelope.setType(type);
        envelope.setPayload(JsonNodeFactory.instance.objectNode());
        return envelope;
    }
}
