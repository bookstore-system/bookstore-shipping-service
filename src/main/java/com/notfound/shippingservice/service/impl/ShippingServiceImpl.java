package com.notfound.shippingservice.service.impl;

import com.notfound.shippingservice.exception.ShippingServiceException;
import com.notfound.shippingservice.model.dto.request.CancelShippingOrderRequest;
import com.notfound.shippingservice.model.dto.request.CreateShippingOrderRequest;
import com.notfound.shippingservice.model.dto.request.ShippingFeeRequest;
import com.notfound.shippingservice.model.dto.response.CancelShippingOrderResponse;
import com.notfound.shippingservice.model.dto.response.CreateShippingOrderResponse;
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

import java.time.OffsetDateTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

    @Value("${shipment.ghn.shop.name:}")
    private String shopName;

    @Value("${shipment.ghn.shop.phone:}")
    private String shopPhone;

    @Value("${shipment.ghn.shop.address:}")
    private String shopAddress;

    @Value("${shipment.ghn.shop.ward:}")
    private String shopWard;

    @Value("${shipment.ghn.shop.district:}")
    private String shopDistrict;

    @Value("${shipment.ghn.shop.province:}")
    private String shopProvince;

    @Override
    public ShippingFeeResponse calculateFee(ShippingFeeRequest request) {
        try {
            Map<String, Object> feeData = getShippingFee(request);
            Map<String, Object> leadTimeData = getLeadTime(request.getToDistrictId(), request.getToWardCode());

            Integer fee = numberValue(feeData.get("total"));
            Integer serviceFee = numberValue(feeData.get("service_fee"));
            Integer insuranceFee = numberValue(feeData.get("insurance_fee"));
            LocalDateTime estimatedDeliveryTime = parseEstimatedDeliveryTime(leadTimeData);
            Integer deliveryDays = calculateDays(estimatedDeliveryTime);

            return ShippingFeeResponse.builder()
                    .totalFee(fee)
                    .serviceFee(serviceFee)
                    .insuranceFee(insuranceFee)
                    .estimatedDeliveryTime(estimatedDeliveryTime)
                    .deliveryDays(deliveryDays)
                    .build();
        } catch (Exception ex) {
            log.error("Failed to calculate shipping fee", ex);
            throw new ShippingServiceException("Failed to calculate shipping fee");
        }
    }

    @Override
    public CreateShippingOrderResponse createOrder(CreateShippingOrderRequest request) {
        validateShopConfiguration();
        try {
            Map<String, Object> body = callGhnCreateOrderApi(buildCreateOrderPayload(request));
            Map<String, Object> data = asMap(body.get("data"));

            return CreateShippingOrderResponse.builder()
                    .orderCode(String.valueOf(data.get("order_code")))
                    .sortingCode(String.valueOf(data.get("sort_code")))
                    .expectedDeliveryTime(parseDateTimeOrNull(data.get("expected_delivery_time")))
                    .totalFee(numberDoubleValue(data.get("total_fee")))
                    .build();
        } catch (Exception ex) {
            log.error("Failed to create shipping order", ex);
            throw new ShippingServiceException("Failed to create shipping order");
        }
    }

    @Override
    public CancelShippingOrderResponse cancelOrder(CancelShippingOrderRequest request) {
        try {
            Map<String, Object> body = callGhnCancelOrderApi(request.getOrderCode());
            List<Map<String, Object>> data = asListOfMap(body.get("data"));
            if (data.isEmpty()) {
                throw new ShippingServiceException("Failed to cancel shipping order");
            }

            Map<String, Object> result = data.get(0);
            Boolean cancelled = Boolean.TRUE.equals(result.get("result"));
            return CancelShippingOrderResponse.builder()
                    .orderCode(request.getOrderCode())
                    .cancelled(cancelled)
                    .message(String.valueOf(result.getOrDefault("message", cancelled ? "Cancelled" : "Cancel failed")))
                    .build();
        } catch (ShippingServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to cancel shipping order {}", request.getOrderCode(), ex);
            throw new ShippingServiceException("Failed to cancel shipping order");
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
                            .supportType(numberValue(item.get("SupportType")))
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
                            .supportType(numberValue(item.get("SupportType")))
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

    private Double numberDoubleValue(Object value) {
        return value == null ? 0D : ((Number) value).doubleValue();
    }

    private LocalDateTime parseEstimatedDeliveryTime(Map<String, Object> leadTimeResponse) {
        if (leadTimeResponse == null || leadTimeResponse.get("data") == null) {
            return null;
        }

        Map<String, Object> data = (Map<String, Object>) leadTimeResponse.get("data");
        Map<String, Object> leadtimeOrder = (Map<String, Object>) data.get("leadtime_order");
        if (leadtimeOrder == null || leadtimeOrder.get("to_estimate_date") == null) {
            return null;
        }

        String toEstimateDate = String.valueOf(leadtimeOrder.get("to_estimate_date"));
        try {
            return LocalDateTime.parse(toEstimateDate, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception ex) {
            try {
                return OffsetDateTime.parse(toEstimateDate, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime();
            } catch (Exception innerEx) {
                log.warn("Unable to parse GHN leadtime: {}", toEstimateDate);
                return null;
            }
        }
    }

    private Integer calculateDays(LocalDateTime estimatedDeliveryTime) {
        if (estimatedDeliveryTime == null) {
            return null;
        }
        return (int) ChronoUnit.DAYS.between(LocalDateTime.now(), estimatedDeliveryTime);
    }

    private void validateShopConfiguration() {
        if (isBlank(shopName) || isBlank(shopPhone) || isBlank(shopAddress)
                || isBlank(shopWard) || isBlank(shopDistrict) || isBlank(shopProvince)) {
            throw new ShippingServiceException("Missing shipment.ghn.shop.* configuration");
        }
    }

    private Map<String, Object> buildCreateOrderPayload(CreateShippingOrderRequest request) {
        return Map.ofEntries(
                Map.entry("payment_type_id", 2),
                Map.entry("required_note", "KHONGCHOXEMHANG"),
                Map.entry("note", Objects.requireNonNullElse(request.getNote(), "Giao hang trong gio hanh chinh")),
                Map.entry("from_name", shopName),
                Map.entry("from_phone", shopPhone),
                Map.entry("from_address", shopAddress),
                Map.entry("from_ward_name", shopWard),
                Map.entry("from_district_name", shopDistrict),
                Map.entry("from_province_name", shopProvince),
                Map.entry("to_name", request.getToName()),
                Map.entry("to_phone", request.getToPhone()),
                Map.entry("to_address", request.getToAddress()),
                Map.entry("to_ward_name", request.getToWardName()),
                Map.entry("to_district_name", request.getToDistrictName()),
                Map.entry("to_province_name", request.getToProvinceName()),
                Map.entry("cod_amount", request.getCodAmount() == null ? 0 : request.getCodAmount()),
                Map.entry("content", Objects.requireNonNullElse(request.getContent(), "Don hang sach")),
                Map.entry("length", request.getLength() == null ? defaultLength : request.getLength()),
                Map.entry("width", request.getWidth() == null ? defaultWidth : request.getWidth()),
                Map.entry("height", request.getHeight() == null ? defaultHeight : request.getHeight()),
                Map.entry("weight", request.getWeight() == null ? defaultWeight : request.getWeight()),
                Map.entry("insurance_value", request.getInsuranceValue() == null ? 0 : request.getInsuranceValue()),
                Map.entry("service_type_id", 2),
                Map.entry("client_order_code", request.getClientOrderCode() == null ? "" : request.getClientOrderCode())
        );
    }

    private Map<String, Object> callGhnCreateOrderApi(Map<String, Object> payload) {
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, buildHeaders(true));
        ResponseEntity<Map> response = restTemplate.exchange(
                ghnApiUrl + "/v2/shipping-order/create",
                HttpMethod.POST,
                entity,
                Map.class
        );

        Map<String, Object> body = asMap(response.getBody());
        if (!Integer.valueOf(200).equals(body.get("code"))) {
            throw new ShippingServiceException("GHN create order API error");
        }
        return body;
    }

    private Map<String, Object> callGhnCancelOrderApi(String orderCode) {
        Map<String, Object> payload = Map.of("order_codes", List.of(orderCode));
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, buildHeaders(true));
        ResponseEntity<Map> response = restTemplate.exchange(
                ghnApiUrl + "/v2/switch-status/cancel",
                HttpMethod.POST,
                entity,
                Map.class
        );

        Map<String, Object> body = asMap(response.getBody());
        if (!Integer.valueOf(200).equals(body.get("code"))) {
            throw new ShippingServiceException("GHN cancel order API error");
        }
        return body;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object value) {
        if (!(value instanceof Map<?, ?> mapValue)) {
            throw new ShippingServiceException("Unexpected GHN response format");
        }
        return (Map<String, Object>) mapValue;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asListOfMap(Object value) {
        if (!(value instanceof List<?> listValue)) {
            throw new ShippingServiceException("Unexpected GHN response format");
        }
        return (List<Map<String, Object>>) listValue;
    }

    private LocalDateTime parseDateTimeOrNull(Object value) {
        if (value == null) {
            return null;
        }
        String dateTime = String.valueOf(value);
        try {
            return LocalDateTime.parse(dateTime, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception ex) {
            try {
                return OffsetDateTime.parse(dateTime, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime();
            } catch (Exception innerEx) {
                log.warn("Unable to parse GHN datetime: {}", dateTime);
                return null;
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
