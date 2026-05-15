package com.notfound.shippingservice.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CancelShippingOrderRequest {
    @NotBlank
    private String orderCode;
}
