# Platform Revenue Management

## Phạm vi

Dashboard này dành riêng cho `MANAGER` để theo dõi doanh số hàng hóa và hoạt
động tổng hợp của toàn sàn. API chỉ đọc dữ liệu, không thay đổi database.

Database hiện chưa có commission hoặc platform fee. Vì vậy API trả
`grossMerchandiseValue` (GMV), `deliveredOrderValue` và
`successfulPaymentAmount` dưới các tên riêng biệt; các giá trị này không phải
lợi nhuận hoặc doanh thu thuần của nền tảng.

## API

```http
GET /api/v1/manager/platform-revenue/dashboard
    ?fromDate=2026-07-01
    &toDate=2026-07-31
    &granularity=DAY
    &topLimit=5
Authorization: Bearer <manager-token>
```

| Query parameter | Bắt buộc | Giá trị |
| --- | --- | --- |
| `fromDate` | Có | Ngày ISO `yyyy-MM-dd`, không ở tương lai |
| `toDate` | Có | Ngày ISO `yyyy-MM-dd`, không ở tương lai |
| `granularity` | Không | `DAY` (mặc định) hoặc `MONTH` |
| `topLimit` | Không | Từ `1` đến `20`, mặc định `5` |

Khoảng báo cáo tối đa là 366 ngày. Hai ngày đầu và cuối đều được tính.

## Response

```json
{
  "success": true,
  "message": "Lấy báo cáo doanh thu toàn sàn thành công.",
  "data": {
    "period": {
      "fromDate": "2026-07-01",
      "toDate": "2026-07-31",
      "granularity": "DAY",
      "generatedAt": "2026-08-01T10:25:00"
    },
    "overview": {
      "grossMerchandiseValue": 125000000.00,
      "previousPeriodGmv": 100000000.00,
      "gmvGrowthPercentage": 25.00,
      "successfulPaymentAmount": 128000000.00,
      "deliveredOrderValue": 127000000.00,
      "totalDiscountAmount": 3000000.00,
      "totalShippingFee": 5000000.00,
      "totalOrders": 850,
      "deliveredOrders": 700,
      "averageOrderValue": 181428.57,
      "unitsSold": 1100,
      "activeSellerCount": 85,
      "activeCustomerCount": 620
    },
    "orderStatusDistribution": [
      {
        "status": "DELIVERED",
        "orderCount": 700,
        "percentage": 82.35
      }
    ],
    "revenueTrend": [
      {
        "periodStart": "2026-07-01",
        "grossMerchandiseValue": 4200000.00,
        "deliveredOrderValue": 4300000.00,
        "deliveredOrders": 24,
        "unitsSold": 39
      }
    ],
    "topSellers": [
      {
        "sellerId": 10,
        "sellerName": "Tech Store",
        "grossMerchandiseValue": 18000000.00,
        "deliveredOrders": 90,
        "unitsSold": 145,
        "marketSharePercentage": 14.40
      }
    ],
    "topProducts": [
      {
        "productId": 15,
        "productName": "Wireless Mouse",
        "sellerId": 10,
        "sellerName": "Tech Store",
        "deliveredOrders": 60,
        "unitsSold": 88,
        "grossMerchandiseValue": 8800000.00
      }
    ],
    "topCategories": [
      {
        "categoryId": 1,
        "categoryName": "Electronics",
        "deliveredOrders": 310,
        "unitsSold": 480,
        "grossMerchandiseValue": 60000000.00,
        "marketSharePercentage": 48.00
      }
    ],
    "paymentMethodDistribution": [
      {
        "paymentMethod": "VNPAY",
        "totalPaymentCount": 500,
        "successfulPaymentCount": 470,
        "pendingPaymentCount": 10,
        "failedPaymentCount": 20,
        "successfulAmount": 90000000.00,
        "percentage": 70.31
      }
    ],
    "platformActivity": {
      "totalSellers": 120,
      "activeSellerAccounts": 105,
      "newSellers": 12,
      "totalCustomers": 1500,
      "activeCustomerAccounts": 1390,
      "newCustomers": 150,
      "totalProducts": 840,
      "activeProducts": 720,
      "inactiveProducts": 70,
      "outOfStockProducts": 50,
      "newProducts": 65,
      "totalCategories": 30,
      "uncategorizedProducts": 8
    },
    "activityTrend": [
      {
        "periodStart": "2026-07-01",
        "newSellers": 2,
        "newCustomers": 18,
        "newProducts": 7
      }
    ]
  }
}
```

Backend luôn điền các mốc ngày hoặc tháng không có dữ liệu bằng `0`. Frontend
không cần tự bổ sung điểm bị thiếu trước khi trực quan hóa.

## Định nghĩa metric

- `grossMerchandiseValue`: tổng `subtotal_amount` của các Order hiện có trạng
  thái `DELIVERED` và được tạo trong khoảng báo cáo.
- `deliveredOrderValue`: tổng `total_amount` của các Order trên, đã gồm shipping
  fee và trừ discount.
- `successfulPaymentAmount`: tổng Payment `SUCCESS` theo thời điểm `paid_at`.
- `averageOrderValue`: `deliveredOrderValue` chia số Order `DELIVERED`.
- `activeSellerCount`: số Seller có sản phẩm trong Order `DELIVERED` của kỳ.
- `activeCustomerCount`: số Customer có Order `DELIVERED` của kỳ.
- `activeSellerAccounts` và `activeCustomerAccounts`: số tài khoản đang có trạng
  thái `ACTIVE`; không đồng nghĩa đã đăng nhập hoặc phát sinh giao dịch.
- `gmvGrowthPercentage`: so sánh với kỳ liền trước có cùng số ngày. Giá trị là
  `null` nếu kỳ trước có GMV bằng 0 và chưa đủ cơ sở so sánh.
- `marketSharePercentage`: tỷ trọng GMV của Seller hoặc Category trên GMV toàn
  sàn trong kỳ.
- `paymentMethodDistribution`: phân bổ các Payment được tạo trong kỳ theo
  `created_at`; `percentage` là tỷ trọng số tiền thành công trong cohort này.

Do schema chưa có `delivered_at`, các metric Order dùng nhóm Order được tạo trong
khoảng ngày chọn và hiện đang có trạng thái `DELIVERED`. Category cũng không
được snapshot trong OrderItem, nên xếp hạng Category dùng Category hiện tại của
Product. Các tổng số User/Product/Category là snapshot trạng thái hiện tại và
loại dữ liệu đã soft-delete; hệ thống chưa có event history để dựng snapshot tại
một ngày trong quá khứ. Payment thành công cũng là gross payment volume vì schema
chưa có số tiền refund để tính dòng tiền ròng.

## Gợi ý bố cục frontend

- KPI cards: GMV, tăng trưởng GMV, giá trị thanh toán thành công, số đơn giao
  thành công, AOV, số sản phẩm bán.
- Time-series: dùng `revenueTrend`; cho phép đổi `DAY`/`MONTH` bằng query param.
- Order status: dùng `orderStatusDistribution`.
- Payment method: dùng `paymentMethodDistribution`.
- Ranking tables: dùng `topSellers`, `topProducts`, `topCategories`.
- Platform activity cards: dùng `platformActivity`.
- Activity time-series: dùng `activityTrend` cho Seller, Customer và Product mới.

Frontend không được hiển thị GMV là lợi nhuận hoặc doanh thu thuần của sàn.
