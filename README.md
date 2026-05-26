# Bookstore Shipping Service

`bookstore-shipping-service` la microservice phu trach tinh phi van chuyen cho he thong Bookstore theo kien truc Microservice.

## 1) Chuc nang

- Cung cap API tinh phi van chuyen theo contract cua he thong:
  - `POST /api/v1/shipping/fee`
- Cung cap API phu tro lay du lieu dia gioi GHN:
  - `GET /api/v1/shipping/provinces`
  - `GET /api/v1/shipping/districts?provinceId=...`
  - `GET /api/v1/shipping/wards?districtId=...`
- Tra ve du lieu theo wrapper `ApiResponse<T>` thong nhat toan he thong.

## 2) Cong nghe

- Java 21
- Spring Boot 4.0.5
- Spring Cloud 2025.1.1
- Docker / Docker Compose
- GHN Public API (shipping provider)

## 3) Chay service

### Cach A: chay local bang Maven

1. Cau hinh bien moi truong GHN (bat buoc de co ket qua that):
   - `GHN_API_TOKEN`
   - `GHN_SHOP_ID`
   - `GHN_FROM_DISTRICT_ID`
   - `GHN_FROM_WARD_CODE`
2. Chay lenh:

```bash
./mvnw spring-boot:run
```

Service chay tai `http://localhost:8080` (host service trong he thong map la `8088`).

### Cach B: chay bang Docker Compose

```bash
docker compose up -d
```

Service map port:
- Host: `8088`
- Container: `8080`

## 4) Test API

### 4.1 Tinh phi van chuyen (endpoint contract chinh)

```bash
curl -X POST http://localhost:8088/api/v1/shipping/fee \
  -H "Content-Type: application/json" \
  -d '{
    "toDistrictId": 1442,
    "toWardCode": "21012",
    "length": 20,
    "width": 15,
    "height": 10,
    "weight": 500,
    "insuranceValue": 100000
  }'
```

Response mau:

```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "fee": 30000,
    "estimatedDays": 3,
    "serviceFee": 25000,
    "insuranceFee": 5000
  }
}
```

### 4.2 Lay danh sach tinh/thanh

```bash
curl http://localhost:8088/api/v1/shipping/provinces
```

### 4.3 Lay danh sach quan/huyen

```bash
curl "http://localhost:8088/api/v1/shipping/districts?provinceId=202"
```

### 4.4 Lay danh sach xa/phuong

```bash
curl "http://localhost:8088/api/v1/shipping/wards?districtId=1442"
```

## 5) Luu y tich hop

- Service name: `bookstore-shipping-service`
- Port convention:
  - host: `8088`
  - container: `8080`
- Endpoint duoc order-service goi de tinh phi:
  - `POST /api/v1/shipping/fee`
- Khong truy cap DB cheo service, shipping giao tiep ngoai qua GHN API.
