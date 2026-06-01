package com.notfound.shippingservice.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentResponse {
    private UUID id;
    private UUID orderId;
    private UUID sagaId;
    private String shippingOrderCode;
    private String status;
    private Double totalFee;
    private LocalDateTime expectedDeliveryTime;
    private String lastError;
}
