package com.notfound.shippingservice;

import com.notfound.shippingservice.config.SagaShippingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@EnableConfigurationProperties(SagaShippingProperties.class)
public class BookstoreShippingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookstoreShippingServiceApplication.class, args);
    }
}
