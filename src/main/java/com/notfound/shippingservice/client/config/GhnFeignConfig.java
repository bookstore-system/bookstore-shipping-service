package com.notfound.shippingservice.client.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class GhnFeignConfig {

    @Bean
    public RequestInterceptor ghnAuthInterceptor(@Value("${shipment.ghn.apiToken:}") String apiToken) {
        return template -> {
            if (apiToken != null && !apiToken.isBlank()) {
                template.header("Token", apiToken);
            }
            template.header("Content-Type", "application/json");
        };
    }
}
