package com.notfound.shippingservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bookstoreShippingServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bookstore Shipping Service")
                        .description("API vận chuyển: phí ship, đơn hàng GHN và địa giới hành chính (microservice).")
                        .version("1.0"));
    }
}
