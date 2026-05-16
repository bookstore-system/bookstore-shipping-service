package com.notfound.shippingservice.client;

import com.notfound.shippingservice.client.config.GhnFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(
        name = "ghn-api-client",
        url = "${clients.ghn.url}",
        configuration = GhnFeignConfig.class
)
public interface GhnApiClient {

    @GetMapping("/master-data/province")
    Map<String, Object> getProvinces();

    @PostMapping("/master-data/district")
    Map<String, Object> getDistricts(@RequestBody Map<String, Object> body);

    @PostMapping("/master-data/ward")
    Map<String, Object> getWards(@RequestBody Map<String, Object> body);

    @PostMapping("/v2/shipping-order/fee")
    Map<String, Object> calculateFee(
            @RequestHeader("ShopId") String shopId,
            @RequestBody Map<String, Object> body
    );

    @PostMapping("/v2/shipping-order/leadtime")
    Map<String, Object> getLeadTime(@RequestBody Map<String, Object> body);

    @PostMapping("/v2/shipping-order/create")
    Map<String, Object> createOrder(
            @RequestHeader("ShopId") String shopId,
            @RequestBody Map<String, Object> body
    );

    @PostMapping("/v2/switch-status/cancel")
    Map<String, Object> cancelOrder(
            @RequestHeader("ShopId") String shopId,
            @RequestBody Map<String, Object> body
    );
}
