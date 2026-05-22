package com.notfound.shippingservice.messaging.dto;

import lombok.Data;
import tools.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class SagaMessageEnvelope {
    private UUID eventId;
    private UUID sagaId;
    private UUID correlationId;
    private UUID causationId;
    private String type;
    private LocalDateTime occurredAt;
    private UUID orderId;
    private String userId;
    private JsonNode payload;
    private String shippingOrderCode;
    private Double totalFee;
    private String expectedDeliveryTime;
    private Integer codAmount;
}
