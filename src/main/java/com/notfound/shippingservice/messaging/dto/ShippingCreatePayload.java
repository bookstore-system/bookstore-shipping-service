package com.notfound.shippingservice.messaging.dto;

import lombok.Data;

@Data
public class ShippingCreatePayload {
    private String toName;
    private String toPhone;
    private String toAddress;
    private String toWardName;
    private String toDistrictName;
    private String toProvinceName;
    private Integer codAmount;
    private String note;
    private String content;
    private Integer length;
    private Integer width;
    private Integer height;
    private Integer weight;
    private Integer insuranceValue;
    private String clientOrderCode;
    private Double expectedShippingFee;
}
