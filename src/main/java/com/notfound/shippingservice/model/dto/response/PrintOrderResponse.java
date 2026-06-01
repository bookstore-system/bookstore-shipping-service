package com.notfound.shippingservice.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrintOrderResponse {
    private List<String> orderCodes;
    private String paperSize;
    private String token;
    private String printUrl;
    private Integer expiresInMinutes;
}
