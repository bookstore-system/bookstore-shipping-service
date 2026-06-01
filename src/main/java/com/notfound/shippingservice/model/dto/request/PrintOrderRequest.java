package com.notfound.shippingservice.model.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrintOrderRequest {
    @NotEmpty
    private List<String> orderCodes;

    private String paperSize;
}
