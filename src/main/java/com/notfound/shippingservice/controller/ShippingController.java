package com.notfound.shippingservice.controller;

import com.notfound.shippingservice.model.dto.request.CancelShippingOrderRequest;
import com.notfound.shippingservice.model.dto.request.CreateShippingOrderRequest;
import com.notfound.shippingservice.model.dto.request.PrintOrderRequest;
import com.notfound.shippingservice.model.dto.request.ShippingFeeRequest;
import com.notfound.shippingservice.model.dto.response.ApiResponse;
import com.notfound.shippingservice.model.dto.response.CancelShippingOrderResponse;
import com.notfound.shippingservice.model.dto.response.CreateShippingOrderResponse;
import com.notfound.shippingservice.model.dto.response.DistrictResponse;
import com.notfound.shippingservice.model.dto.response.PrintOrderResponse;
import com.notfound.shippingservice.model.dto.response.ProvinceResponse;
import com.notfound.shippingservice.model.dto.response.ShipmentResponse;
import com.notfound.shippingservice.model.dto.response.ShippingFeeResponse;
import com.notfound.shippingservice.model.dto.response.WardResponse;
import com.notfound.shippingservice.service.ShippingService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shipment")
@RequiredArgsConstructor
@Tag(name = "Shipment", description = "Tỉnh, huyện, xã và tính phí giao hàng")
public class ShippingController {
    private final ShippingService shippingService;

    @GetMapping("/customer/province")
    public ResponseEntity<ApiResponse<List<ProvinceResponse>>> getProvinces() {
        return ResponseEntity.ok(ApiResponse.success(shippingService.getProvinces()));
    }

    @GetMapping("/customer/district")
    public ResponseEntity<ApiResponse<List<DistrictResponse>>> getDistricts(@RequestParam Integer provinceId) {
        return ResponseEntity.ok(ApiResponse.success(shippingService.getDistricts(provinceId)));
    }

    @GetMapping("/customer/ward")
    public ResponseEntity<ApiResponse<List<WardResponse>>> getWards(@RequestParam Integer districtId) {
        return ResponseEntity.ok(ApiResponse.success(shippingService.getWards(districtId)));
    }

    @PostMapping("/customer/calculate")
    public ResponseEntity<ApiResponse<ShippingFeeResponse>> calculateFee(@RequestBody @Valid ShippingFeeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(shippingService.calculateFee(request)));
    }

    @PostMapping("/order")
    public ResponseEntity<ApiResponse<CreateShippingOrderResponse>> createOrder(@RequestBody @Valid CreateShippingOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(shippingService.createOrder(request)));
    }

    @PostMapping("/order/cancel")
    public ResponseEntity<ApiResponse<CancelShippingOrderResponse>> cancelOrder(@RequestBody @Valid CancelShippingOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(shippingService.cancelOrder(request)));
    }

    @GetMapping("/order/by-order/{orderId}")
    public ResponseEntity<ApiResponse<ShipmentResponse>> getShipmentByOrderId(@org.springframework.web.bind.annotation.PathVariable UUID orderId) {
        return ResponseEntity.ok(ApiResponse.success(shippingService.getShipmentByOrderId(orderId)));
    }

    @PostMapping("/order/print")
    public ResponseEntity<ApiResponse<PrintOrderResponse>> generatePrintOrder(@RequestBody @Valid PrintOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(shippingService.generatePrintOrder(request)));
    }

    @GetMapping("/order/print")
    public ResponseEntity<ApiResponse<PrintOrderResponse>> generatePrintOrder(
            @RequestParam String orderCode,
            @RequestParam(defaultValue = "A5") String paperSize) {
        PrintOrderRequest request = PrintOrderRequest.builder()
                .orderCodes(List.of(orderCode))
                .paperSize(paperSize)
                .build();
        return ResponseEntity.ok(ApiResponse.success(shippingService.generatePrintOrder(request)));
    }

    @GetMapping("/order/{orderId}/print")
    public ResponseEntity<ApiResponse<PrintOrderResponse>> generatePrintOrderByOrderId(
            @org.springframework.web.bind.annotation.PathVariable UUID orderId,
            @RequestParam(defaultValue = "A5") String paperSize) {
        return ResponseEntity.ok(ApiResponse.success(shippingService.generatePrintOrderByOrderId(orderId, paperSize)));
    }
}
