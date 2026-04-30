package com.notfound.shippingservice.service.impl;

import com.notfound.shippingservice.exception.ShippingServiceException;
import com.notfound.shippingservice.model.dto.request.ShippingFeeRequest;
import com.notfound.shippingservice.model.dto.response.DistrictResponse;
import com.notfound.shippingservice.model.dto.response.ProvinceResponse;
import com.notfound.shippingservice.model.dto.response.ShippingFeeResponse;
import com.notfound.shippingservice.model.dto.response.WardResponse;
import com.notfound.shippingservice.service.ShippingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShippingServiceImpl implements ShippingService {
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${shipment.ghn.url}")
    private String ghnApiUrl;

    @Value("${shipment.ghn.apiToken}")
    private String ghnToken;

    @Value("${shipment.ghn.shopId}")
    private String shopId;

    @Value("${shipment.ghn.address.fromDistrictId}")
    private Integer fromDistrictId;

    @Value("${shipment.ghn.address.fromWardCode}")
    private String fromWardCode;

    @Value("${shipment.ghn.defaultBook.length:20}")
    private Integer defaultLength;

    @Value("${shipment.ghn.defaultBook.width:15}")
    private Integer defaultWidth;

    @Value("${shipment.ghn.defaultBook.height:10}")
    private Integer defaultHeight;

    @Value("${shipment.ghn.defaultBook.weight:500}")
    private Integer defaultWeight;

    @Override
    public ShippingFeeResponse calculateFee(ShippingFeeRequest request) {
        try {
            Map<String, Object> feeData = getShippingFee(request);
            Map<String, Object> leadTimeData = getLeadTime(request.getToDistrictId(), request.getToWardCode());

            Integer fee = numberValue(feeData.get("total"));
            Integer serviceFee = numberValue(feeData.get("service_fee"));
            Integer insuranceFee = numberValue(feeData.get("insurance_fee"));
            Integer estimatedDays = calculateDays(leadTimeData);

            return ShippingFeeResponse.builder()
                    .fee(fee)
                    .serviceFee(serviceFee)
                    .insuranceFee(insuranceFee)
                    .estimatedDays(estimatedDays)
                    .build();
        } catch (Exception ex) {
            log.error("Failed to calculate shipping fee", ex);
            throw new ShippingServiceException("Failed to calculate shipping fee");
        }
    }

    @Override
    public List<ProvinceResponse> getProvinces() {
        try {
            HttpEntity<Void> entity = new HttpEntity<>(buildHeaders(false));
            ResponseEntity<Map> response = restTemplate.exchange(
                    ghnApiUrl + "/master-data/province",
                    HttpMethod.GET,
                    entity,
                    Map.class
            );
            List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
            return data.stream()
                    .filter(item -> numberValue(item.get("Status")) == 1)
                    .map(item -> ProvinceResponse.builder()
                            .provinceId(numberValue(item.get("ProvinceID")))
                            .provinceName(String.valueOf(item.get("ProvinceName")))
                            .code(String.valueOf(item.get("Code")))
                            .build())
                    .toList();
        } catch (Exception ex) {
            log.error("Failed to get provinces", ex);
            throw new ShippingServiceException("Failed to get provinces");
        }
    }

    @Override
    public List<DistrictResponse> getDistricts(Integer provinceId) {
        try {
            Map<String, Object> body = Map.of("province_id", provinceId);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildHeaders(false));
            ResponseEntity<Map> response = restTemplate.exchange(
                    ghnApiUrl + "/master-data/district",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );
            List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
            return data.stream()
                    .filter(item -> numberValue(item.get("Status")) == 1)
                    .map(item -> DistrictResponse.builder()
                            .districtId(numberValue(item.get("DistrictID")))
                            .provinceId(numberValue(item.get("ProvinceID")))
                            .districtName(String.valueOf(item.get("DistrictName")))
                            .build())
                    .toList();
        } catch (Exception ex) {
            log.error("Failed to get districts for province {}", provinceId, ex);
            throw new ShippingServiceException("Failed to get districts");
        }
    }

    @Override
    public List<WardResponse> getWards(Integer districtId) {
        try {
            Map<String, Object> body = Map.of("district_id", districtId);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildHeaders(false));
            ResponseEntity<Map> response = restTemplate.exchange(
                    ghnApiUrl + "/master-data/ward",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );
            List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
            return data.stream()
                    .filter(item -> numberValue(item.get("Status")) == 1)
                    .map(item -> WardResponse.builder()
                            .wardCode(String.valueOf(item.get("WardCode")))
                            .districtId(numberValue(item.get("DistrictID")))
                            .wardName(String.valueOf(item.get("WardName")))
                            .build())
                    .toList();
        } catch (Exception ex) {
            log.error("Failed to get wards for district {}", districtId, ex);
            throw new ShippingServiceException("Failed to get wards");
        }
    }

    private Map<String, Object> getShippingFee(ShippingFeeRequest request) {
        Map<String, Object> body = Map.of(
                "service_type_id", 2,
                "from_district_id", fromDistrictId,
                "from_ward_code", fromWardCode,
                "to_district_id", request.getToDistrictId(),
                "to_ward_code", request.getToWardCode(),
                "length", request.getLength() == null ? defaultLength : request.getLength(),
                "width", request.getWidth() == null ? defaultWidth : request.getWidth(),
                "height", request.getHeight() == null ? defaultHeight : request.getHeight(),
                "weight", request.getWeight() == null ? defaultWeight : request.getWeight(),
                "insurance_value", request.getInsuranceValue() == null ? 0 : request.getInsuranceValue()
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildHeaders(true));
        ResponseEntity<Map> response = restTemplate.exchange(
                ghnApiUrl + "/v2/shipping-order/fee",
                HttpMethod.POST,
                entity,
                Map.class
        );
        return (Map<String, Object>) response.getBody().get("data");
    }

    private Map<String, Object> getLeadTime(Integer toDistrictId, String toWardCode) {
        Map<String, Object> body = Map.of(
                "from_district_id", fromDistrictId,
                "from_ward_code", fromWardCode,
                "to_district_id", toDistrictId,
                "to_ward_code", toWardCode,
                "service_id", 53320
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildHeaders(false));
        ResponseEntity<Map> response = restTemplate.exchange(
                ghnApiUrl + "/v2/shipping-order/leadtime",
                HttpMethod.POST,
                entity,
                Map.class
        );
        return response.getBody();
    }

    private HttpHeaders buildHeaders(boolean includeShopId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", ghnToken);
        if (includeShopId) {
            headers.set("ShopId", shopId);
        }
        return headers;
    }

    private Integer numberValue(Object value) {
        return value == null ? 0 : ((Number) value).intValue();
    }

    private Integer calculateDays(Map<String, Object> leadTimeResponse) {
        if (leadTimeResponse == null || leadTimeResponse.get("data") == null) {
            return null;
        }
        Map<String, Object> data = (Map<String, Object>) leadTimeResponse.get("data");
        Map<String, Object> leadtimeOrder = (Map<String, Object>) data.get("leadtime_order");
        if (leadtimeOrder == null || leadtimeOrder.get("to_estimate_date") == null) {
            return null;
        }
        LocalDateTime estimated = LocalDateTime.parse(String.valueOf(leadtimeOrder.get("to_estimate_date")));
        return (int) ChronoUnit.DAYS.between(LocalDateTime.now(), estimated);
    }
}
