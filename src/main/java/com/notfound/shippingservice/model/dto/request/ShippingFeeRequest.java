package com.notfound.shippingservice.model.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ShippingFeeRequest {
    @NotNull
    private Integer toDistrictId;

    @NotBlank
    private String toWardCode;

    @Min(1)
    private Integer length = 20;

    @Min(1)
    private Integer width = 15;

    @Min(1)
    private Integer height = 10;

    @Min(1)
    private Integer weight = 500;

    @Min(0)
    private Integer insuranceValue = 0;
}
