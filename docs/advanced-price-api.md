# API Gợi ý Giá nâng cao

Base path: `/api/v1/dss/advanced-price/sessions`  
Quyền truy cập: `SELLER`

## Luồng sử dụng

### 1. Tạo phiên phân tích

`POST /api/v1/dss/advanced-price/sessions`

```json
{
  "productId": 15,
  "fromDate": "2026-03-01",
  "toDate": "2026-07-31",
  "forecastPeriod": 14,
  "estimatedOrderCost": 25000
}
```

- Khoảng lịch sử: từ 7 đến 180 ngày.
- `forecastPeriod`: chỉ nhận `7`, `14` hoặc `30`.
- Các giá trị trên được cố định theo `sessionId`.
- Nhu cầu nền dùng chung `DemandForecastEngine` và model LightGBM ONNX.
- `forecastMethod` cho biết model thật sự được dùng hay fallback.
- E ưu tiên tính trong khoảng A–B; nếu khoảng đó không có đủ mức giá,
  Backend dùng toàn bộ lịch sử và trả `elasticitySource=ALL_HISTORY_FALLBACK`.

### 2. Tạo hoặc cập nhật bảng so sánh

`POST /api/v1/dss/advanced-price/sessions/{sessionId}/scenarios`

```json
{
  "priceChangePercent": -15
}
```

- Miền giá trị: `-70` đến `100`.
- `-15` nghĩa là giảm 15%; `20` nghĩa là tăng 20%.
- Phần trăm trong cùng phiên không được trùng nhau.
- Backend giữ tối đa 5 kịch bản gần nhất; kịch bản thứ 6 thay kịch bản cũ nhất.

## Công thức

```text
giá mới = giá hiện tại × (1 + % đổi giá)
hệ số nhu cầu = max(0, 1 + E × % đổi giá)
nhu cầu dự báo = nhu cầu LightGBM × hệ số nhu cầu
LN/SP = giá mới - giá vốn - chi phí người bán nhập
LN kỳ vọng = nhu cầu dự báo × LN/SP
```

## 3. Lấy lại phiên và 5 kịch bản

`GET /api/v1/dss/advanced-price/sessions/{sessionId}`

Mảng `scenarios` được trả theo thứ tự mới nhất trước; `latestScenario` dùng cho
khối chi tiết trên giao diện.

## 4. Áp dụng giá

`POST /api/v1/dss/advanced-price/sessions/{sessionId}/scenarios/{scenarioId}/apply`

Backend kiểm tra quyền sở hữu và giá hiện tại. Nếu giá sản phẩm đã thay đổi từ
lúc tạo phiên, yêu cầu bị từ chối và Seller phải tạo phiên mới. Khi thành công,
giá sản phẩm và `price_history` được cập nhật trong cùng transaction; phiên được
chuyển sang trạng thái `APPLIED`.

