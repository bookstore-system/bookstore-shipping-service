package com.notfound.shippingservice.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DistrictResponse {
    private Integer districtId;
    private Integer provinceId;
    private String districtName;
    private Integer supportType;
}
