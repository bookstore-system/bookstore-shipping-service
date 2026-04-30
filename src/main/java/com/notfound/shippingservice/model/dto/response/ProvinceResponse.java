package com.notfound.shippingservice.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProvinceResponse {
    private Integer provinceId;
    private String provinceName;
    private String code;
}
