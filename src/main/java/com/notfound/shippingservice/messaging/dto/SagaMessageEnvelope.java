package com.notfound.shippingservice.messaging.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class SagaMessageEnvelope {
    private UUID eventId;
    private UUID sagaId;
    private UUID correlationId;
    private UUID causationId;
    private String type;
    private Instant occurredAt;
    private UUID orderId;
    private String userId;
    private JsonNode payload;
}
