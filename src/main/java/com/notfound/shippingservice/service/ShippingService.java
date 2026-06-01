package com.notfound.shippingservice.service;

import com.notfound.shippingservice.model.dto.request.CancelShippingOrderRequest;
import com.notfound.shippingservice.model.dto.request.CreateShippingOrderRequest;
import com.notfound.shippingservice.model.dto.request.PrintOrderRequest;
import com.notfound.shippingservice.model.dto.request.ShippingFeeRequest;
import com.notfound.shippingservice.model.dto.response.CancelShippingOrderResponse;
import com.notfound.shippingservice.model.dto.response.CreateShippingOrderResponse;
import com.notfound.shippingservice.model.dto.response.DistrictResponse;
import com.notfound.shippingservice.model.dto.response.PrintOrderResponse;
import com.notfound.shippingservice.model.dto.response.ProvinceResponse;
import com.notfound.shippingservice.model.dto.response.ShipmentResponse;
import com.notfound.shippingservice.model.dto.response.ShippingFeeResponse;
import com.notfound.shippingservice.model.dto.response.WardResponse;

import java.util.List;
import java.util.UUID;

public interface ShippingService {
    ShippingFeeResponse calculateFee(ShippingFeeRequest request);
    CreateShippingOrderResponse createOrder(CreateShippingOrderRequest request);
    CancelShippingOrderResponse cancelOrder(CancelShippingOrderRequest request);
    ShipmentResponse getShipmentByOrderId(UUID orderId);
    PrintOrderResponse generatePrintOrder(PrintOrderRequest request);
    PrintOrderResponse generatePrintOrderByOrderId(UUID orderId, String paperSize);
    List<ProvinceResponse> getProvinces();
    List<DistrictResponse> getDistricts(Integer provinceId);
    List<WardResponse> getWards(Integer districtId);
}
