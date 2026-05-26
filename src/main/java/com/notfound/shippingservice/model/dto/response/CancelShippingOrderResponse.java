package com.notfound.shippingservice.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CancelShippingOrderResponse {
    private String orderCode;
    private Boolean cancelled;
    private String message;
}
