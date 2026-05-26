package com.notfound.shippingservice.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CreateShippingOrderResponse {
    private String orderCode;
    private String sortingCode;
    private LocalDateTime expectedDeliveryTime;
    private Double totalFee;
}
