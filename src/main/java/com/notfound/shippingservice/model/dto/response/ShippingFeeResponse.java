package com.notfound.shippingservice.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShippingFeeResponse {
    private Integer fee;
    private Integer estimatedDays;
    private Integer serviceFee;
    private Integer insuranceFee;
}
