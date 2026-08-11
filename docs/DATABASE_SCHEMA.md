# SEDSP — Mô tả schema cơ sở dữ liệu

> **Tách biệt với ERD:** File này mô tả **bảng, cột, ràng buộc**. Sơ đồ quan hệ (ERD) nằm tại [`DATABASE_ERD.md`](./DATABASE_ERD.md).

## Tổng quan

- **Hệ quản trị:** PostgreSQL (Railway production)
- **Migration:** Flyway `backend/src/main/resources/db/migration/V*.sql`
- **Soft-delete:** `deleted_at` trên `users`, `products`, `cart_items`, `product_reviews`, …

## Nhóm bảng chính

### Xác thực & người dùng

| Bảng | PK | Ghi chú |
|------|-----|---------|
| `roles` | `id` | ADMIN, SELLER, CUSTOMER, MANAGER |
| `users` | `id` | FK `role_id` → `roles`; UNIQUE partial `email/username/phone` WHERE `deleted_at IS NULL` (V55) |

### Catalog

| Bảng | PK | FK chính |
|------|-----|----------|
| `categories` | `id` | self `parent_id` |
| `products` | `id` | `seller_id` → `users`, `category_id` → `categories`; **`cost_price`** dùng cho DSS lợi nhuận |
| `product_images` | `id` | `product_id` |
| `inventory` | `id` | `product_id` |
| `price_history` | `id` | `product_id` — lịch sử đổi giá cho elasticity |

### Đơn hàng & thanh toán

| Bảng | PK | FK / cột DSS |
|------|-----|----------------|
| `orders` | `id` | `user_id`; **`shipping_fee`** (theo đơn, chưa phân bổ theo SP) |
| `order_items` | `id` | `order_id`, `product_id`, `quantity`, `unit_price_at_purchase` |
| `payments` | `id` | `order_id` |

**DSS formal services** lọc đơn **`DELIVERED`** khi tính nhu cầu/lịch sử bán.

### DSS

| Bảng | PK | Ghi chú |
|------|-----|---------|
| `demand_predictions` | `id` | Lưu dự báo nhu cầu (POST API) |
| `dss_scenarios` / `dss_results` | — | Schema V20 — **chưa wired** trong Java hiện tại |

## Chi phí & DSS (dữ liệu thật vs cấu hình)

| Khoản | Nguồn |
|-------|--------|
| Giá vốn (COGS) | `products.cost_price` |
| Doanh thu | `price × quantity` (kịch bản) |
| Phí giao hàng | **Cấu hình** `app.dss.avg-shipping-per-unit-vnd` (ước tính; `orders.shipping_fee` theo đơn) |
| Phí nền tảng | **Cấu hình** `app.dss.platform-fee-percent` |
| Chi phí vận hành | **Cấu hình** `app.dss.operating-cost-per-unit-vnd` (mặc định 0) |

## Ràng buộc quan trọng

- `users.email`, `users.username`, `users.phone`: unique chỉ trên bản ghi **active** (`deleted_at IS NULL`)
- `products`: seller ownership qua `seller_id`
- `order_items.product_id` → FK sản phẩm

## Cách cập nhật tài liệu

Khi thêm migration mới, cập nhật file này **và** ERD riêng — không gộp schema + diagram vào một hình duy nhất.
