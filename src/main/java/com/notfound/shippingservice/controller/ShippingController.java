package com.notfound.shippingservice.controller;

import com.notfound.shippingservice.model.dto.request.ShippingFeeRequest;
import com.notfound.shippingservice.model.dto.response.ApiResponse;
import com.notfound.shippingservice.model.dto.response.DistrictResponse;
import com.notfound.shippingservice.model.dto.response.ProvinceResponse;
import com.notfound.shippingservice.model.dto.response.ShippingFeeResponse;
import com.notfound.shippingservice.model.dto.response.WardResponse;
import com.notfound.shippingservice.service.ShippingService;
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

@RestController
@RequestMapping("/api/v1/shipping")
@RequiredArgsConstructor
public class ShippingController {
    private final ShippingService shippingService;

    @PostMapping("/fee")
    public ResponseEntity<ApiResponse<ShippingFeeResponse>> calculateFee(@RequestBody @Valid ShippingFeeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(shippingService.calculateFee(request)));
    }

    @GetMapping("/provinces")
    public ResponseEntity<ApiResponse<List<ProvinceResponse>>> getProvinces() {
        return ResponseEntity.ok(ApiResponse.success(shippingService.getProvinces()));
    }

    @GetMapping("/districts")
    public ResponseEntity<ApiResponse<List<DistrictResponse>>> getDistricts(@RequestParam Integer provinceId) {
        return ResponseEntity.ok(ApiResponse.success(shippingService.getDistricts(provinceId)));
    }

    @GetMapping("/wards")
    public ResponseEntity<ApiResponse<List<WardResponse>>> getWards(@RequestParam Integer districtId) {
        return ResponseEntity.ok(ApiResponse.success(shippingService.getWards(districtId)));
    }
}
