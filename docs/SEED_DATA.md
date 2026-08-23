# Seed dữ liệu SEDSP (master)

## Nguồn dữ liệu chuẩn

Toàn bộ dữ liệu demo/marketplace được quản lý qua **Flyway migrations** (khuyến nghị cho Railway/Vercel):

| Migration | Nội dung |
|-----------|----------|
| V24 | Users dev, catalog nhỏ |
| V25–V27 | Categories marketplace, 50 SP demo |
| V28–V29 | Ảnh, inventory, attributes, orders |
| V36, V39, V43, V46 | DSS sales / platform demo |
| V47 | Việt hóa label + backfill tồn kho |
| V48 | Cleanup attributes, nhà cung cấp, category |
| V51 | 180 ngày nhu cầu dự báo: tăng, giảm, ổn định theo tuần, bán gián đoạn |
| V65 | seller@sedsp.vn — seed 180 ngày trên 4 SP catalog có sẵn (không tạo SP mới) |

Chạy `flyway migrate` (tự động khi boot BE) sẽ áp dụng đầy đủ.

## Sản phẩm kiểm thử dự báo nhu cầu (seller@sedsp.vn)

Đăng nhập **seller@sedsp.vn** / **12345678** → DSS → **Dự báo nhu cầu**:

| Sản phẩm | Slug | Xu hướng DSS |
|----------|------|----------------|
| Bàn phím cơ RGB KeyPro K87 | `ban-phim-co-rgb-keypro-k87` | Đang tăng |
| Tai nghe Bluetooth Pro ANC | `tai-nghe-bluetooth-pro-anc` | Đang giảm |
| Nồi chiên không dầu 5L | `noi-chien-khong-dau-5l` | Tương đối ổn định |
| Giày chạy bộ AirFlex Marathon | `giay-chay-bo-airflex-marathon` | Bán gián đoạn |

Dữ liệu bán: **180 ngày** kết thúc **hôm nay**. SP `DSS Forecast - …` / `seller-sedsp-trend-*` đã gỡ.

## Export full DB (cho teammate cập nhật)

### Cách 1 — Admin API

1. Đăng nhập **admin**
2. `GET /api/v1/admin/seed-data/status`
3. `GET /api/v1/admin/seed-data/download` → `sedsp_seed.sql`
4. Commit vào `backend/src/main/resources/db/seed/sedsp_seed.sql`

### Cách 2 — pg_dump local

```bash
pg_dump -h localhost -U postgres -d sedsp --data-only --column-inserts -f backend/src/main/resources/db/seed/sedsp_seed.sql
```

### Import (chỉ dev)

`POST /api/v1/admin/seed-data/import` hoặc `psql -f sedsp_seed.sql`

## Tài khoản demo

| Vai trò | Email | Mật khẩu |
|---------|-------|----------|
| Customer | customer@secdsp.com | 12345678 |
| Seller | seller01@secdsp.com | 12345678 |
| Manager | manager@secdsp.com | 12345678 |
| Admin | admin@secdsp.com | 12345678 |
| DSS Seller | seller.dss.demo@example.com | password |

UI login: giữ **Ctrl** + bấm **Đăng nhập** để hiện nút demo.

## Cập nhật an toàn

1. Thêm migration mới theo số phiên bản tiếp theo (idempotent)
2. Push `Minhedit` / `railway`
3. Không ghi đè production bằng dump cũ nếu đã có đơn thật
