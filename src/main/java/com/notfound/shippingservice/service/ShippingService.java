package com.notfound.shippingservice.service;

import com.notfound.shippingservice.model.dto.request.ShippingFeeRequest;
import com.notfound.shippingservice.model.dto.response.DistrictResponse;
import com.notfound.shippingservice.model.dto.response.ProvinceResponse;
import com.notfound.shippingservice.model.dto.response.ShippingFeeResponse;
import com.notfound.shippingservice.model.dto.response.WardResponse;

import java.util.List;

public interface ShippingService {
    ShippingFeeResponse calculateFee(ShippingFeeRequest request);
    List<ProvinceResponse> getProvinces();
    List<DistrictResponse> getDistricts(Integer provinceId);
    List<WardResponse> getWards(Integer districtId);
}
