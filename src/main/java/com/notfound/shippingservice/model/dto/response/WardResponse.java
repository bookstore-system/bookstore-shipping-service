package com.notfound.shippingservice.model.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WardResponse {
    private String wardCode;
    private Integer districtId;
    private String wardName;
}
