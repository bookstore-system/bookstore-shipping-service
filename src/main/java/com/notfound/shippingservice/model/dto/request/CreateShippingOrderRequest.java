package com.notfound.shippingservice.model.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateShippingOrderRequest {
    @NotBlank
    @Size(max = 1024)
    private String toName;

    @NotBlank
    @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$")
    private String toPhone;

    @NotBlank
    @Size(max = 1024)
    private String toAddress;

    @NotBlank
    private String toWardName;

    @NotBlank
    private String toDistrictName;

    @NotBlank
    private String toProvinceName;

    @Min(0)
    @Max(50000000)
    private Integer codAmount = 0;

    @Size(max = 5000)
    private String note;

    @Size(max = 2000)
    private String content;

    @Min(1)
    @Max(200)
    private Integer length = 20;

    @Min(1)
    @Max(200)
    private Integer width = 15;

    @Min(1)
    @Max(200)
    private Integer height = 10;

    @Min(1)
    @Max(50000)
    private Integer weight = 500;

    @Min(0)
    @Max(5000000)
    private Integer insuranceValue = 0;

    @Size(max = 50)
    private String clientOrderCode;

    @Min(0)
    private Double fallbackShippingFee;
}
