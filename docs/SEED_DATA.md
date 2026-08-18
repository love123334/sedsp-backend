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

Chạy `flyway migrate` (tự động khi boot BE) sẽ áp dụng đầy đủ.

## Sản phẩm kiểm thử dự báo nhu cầu

Đăng nhập seller DSS và chọn một trong bốn sản phẩm sau:

| Sản phẩm | Slug | Mẫu dữ liệu 180 ngày |
|----------|------|----------------------|
| DSS Forecast - Nhu cầu tăng | `dss-forecast-growing-demand` | Tăng dần, cao hơn vào cuối tuần |
| DSS Forecast - Nhu cầu giảm | `dss-forecast-declining-demand` | Giảm dần theo thời gian |
| DSS Forecast - Ổn định theo tuần | `dss-forecast-weekly-stable-demand` | Mức nền ổn định, có chu kỳ cuối tuần |
| DSS Forecast - Bán gián đoạn | `dss-forecast-intermittent-demand` | Có ngày bằng 0, xu hướng tăng nhẹ |

Các sản phẩm này hỗ trợ kiểm thử lookback 7, 14, 30, 60 và 180 ngày.

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
