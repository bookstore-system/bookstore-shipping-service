package com.notfound.shippingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class BookstoreShippingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookstoreShippingServiceApplication.class, args);
    }
}
