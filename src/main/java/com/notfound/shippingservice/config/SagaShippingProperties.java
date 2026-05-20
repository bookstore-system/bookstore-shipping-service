package com.notfound.shippingservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "bookstore.saga")
public class SagaShippingProperties {
    private boolean enabled = true;
    private String commandsExchange = "bookstore.commands";
    private String eventsExchange = "bookstore.events";
    private String commandsQueue = "shipping.commands.queue";
    private int createRetryMax = 2;
    private long createRetryDelayMs = 1500;
}
