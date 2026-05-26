package com.notfound.shippingservice.messaging.dto;

import lombok.Data;

@Data
public class ShippingCancelPayload {
    private String shippingOrderCode;
    private String reason;
}
