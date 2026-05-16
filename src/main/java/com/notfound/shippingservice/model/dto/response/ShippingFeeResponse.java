package com.notfound.shippingservice.model.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ShippingFeeResponse {
    private Integer totalFee;
    private Integer serviceFee;
    private Integer insuranceFee;
    private LocalDateTime estimatedDeliveryTime;
    private Integer deliveryDays;
}
