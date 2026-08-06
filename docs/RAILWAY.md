# Deploy SEDSP Backend trên Railway

## Lỗi healthcheck lặp lại (DATABASE_* = UNSET/EMPTY)

Triệu chứng trong Deploy Logs:

```text
[env] DATABASE_URL=UNSET   (hoặc EMPTY)
[env] DATABASE_PUBLIC_URL=UNSET
[datasource] WARN: no DB URL/PGHOST
```

**Nguyên nhân thường gặp:** trên `sedsp-api` chỉ reference `DATABASE_URL` (private).
Private URL hay bị **rỗng** nếu private networking chưa sẵn → container không có JDBC → app không boot → Railway healthcheck fail ~5 phút.

### Fix (bắt buộc)

1. Mở **sedsp-api → Variables**
2. **New Variable → Add Reference** (không gõ tay URL):
   - Name: `DATABASE_PUBLIC_URL`
   - Value: chọn service Postgres (vd. `sedsp-db`) → `DATABASE_PUBLIC_URL`
3. (Khuyến nghị thêm backup)
   - `PGHOST`, `PGPORT`, `PGUSER`, `PGPASSWORD`, `PGDATABASE` từ cùng Postgres
4. Redeploy `sedsp-api`

Sau khi đúng, logs phải có:

```text
[env] DATABASE_PUBLIC_URL=SET len=…
[env] selected DATABASE_PUBLIC_URL
[env] JDBC ready from DATABASE_PUBLIC_URL
[datasource] OK url=jdbc:postgresql://***@…
```

Health: `https://YOUR-APP.up.railway.app/actuator/health/liveness` → `{"status":"UP"}`

---

## Lỗi Tomcat / `jpaSharedEM_entityManagerFactory` (boot crash sau khi DB đã connect)

Triệu chứng:

```text
[datasource] OK url=jdbc:postgresql://…
Flyway: Database: jdbc:postgresql://… (PostgreSQL 18.x)
Flyway upgrade recommended: PostgreSQL 18…
Error creating bean … 'jpaSharedEM_entityManagerFactory'
```

**Nguyên nhân thường gặp**

1. **Flyway migration fail** (checksum V24/V35, enum payment, …) → JPA không tạo được `EntityManagerFactory` (lỗi Tomcat chỉ là hệ quả).
2. Postgres **18** trên Railway + Flyway cũ → warning / edge-case; app đã pin Flyway **11.14.1**.
3. `ddl-auto: validate` lệch schema nhẹ → crash; prod đã chuyển **`ddl-auto: none`** (Flyway là source of truth).

**Fix**

1. Redeploy `sedsp-api` (nhánh `railway` / `main` mới nhất).
2. Trong Deploy Logs tìm dòng `==== SEDSP STARTUP FAILED — cause chain ====` (root cause Flyway/SQL).
3. Nếu vẫn fail: Railway → Postgres → Query:
   - `SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 10;`
   - Xóa/repair dòng `success = false`, hoặc chạy lại deploy (app có `repair-on-migrate: true`).
4. Ưu tiên reference **private** `DATABASE_URL` (nội bộ cùng project) — nhanh hơn `DATABASE_PUBLIC_URL` và ít treo SSL lúc Flyway. Public URL chỉ là fallback.
5. Service Variables: đảm bảo có `PORT=8080` (hoặc để Railway inject). Healthcheck timeout app đặt **600s**.
6. Deploy Logs (không phải Build Logs) mới có `==== SEDSP STARTUP FAILED — cause chain ====`.

---

## Setup nhanh

1. GitHub → `love123334/sedsp-backend` branch **`railway`** hoặc **`main`**
2. Thêm **PostgreSQL** trong cùng project
3. Variables trên API như bảng dưới + `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`
4. Networking → Generate Domain  
5. **Custom Start Command**: để trống (dùng Dockerfile `ENTRYPOINT`)

| Tên trên API | Reference từ Postgres |
|--------------|------------------------|
| `DATABASE_PUBLIC_URL` | `DATABASE_PUBLIC_URL` (**bắt buộc**) |
| `DATABASE_URL` | `DATABASE_URL` (optional) |
| `PGHOST` / `PGPORT` / `PGUSER` / `PGPASSWORD` / `PGDATABASE` | cùng tên |

```env
SPRING_PROFILES_ACTIVE=prod
JAVA_OPTS=-Xms256m -Xmx512m -Duser.timezone=Asia/Ho_Chi_Minh
JWT_SECRET=<chuỗi ≥32 ký tự>
CORS_ALLOWED_ORIGINS=https://YOUR-VERCEL.vercel.app,https://*.vercel.app,http://localhost:5173
FRONTEND_BASE_URL=https://YOUR-VERCEL.vercel.app

# AI DSS commentary (OpenRouter) — bắt buộc nếu muốn nhận xét AI trên Vercel
AI_ENABLED=true
OPENROUTER_API_KEY=sk-or-v1-...
# AI_MODEL=openrouter/free

# --- OTP email (Railway Hobby blocks outbound SMTP 25/465/587) ---
# Dùng Resend HTTPS API — bắt buộc trên Free/Trial/Hobby:
RESEND_API_KEY=re_xxxxxxxx
# Test (chưa verify domain): chỉ gửi được tới email chủ Resend account
MAIL_FROM=SEDSP <onboarding@resend.dev>
# Prod (đã verify domain trên resend.com/domains):
# MAIL_FROM=SEDSP <noreply@yourdomain.com>

# Gmail SMTP chỉ dùng được local / Railway Pro — Hobby sẽ Connect timed out:
# MAIL_HOST=smtp.gmail.com
# MAIL_PORT=587
# MAIL_USERNAME=sedsp.official@gmail.com
# MAIL_PASSWORD=<gmail-app-password>
# MAIL_FROM=sedsp.official@gmail.com

# VNPay sandbox (không commit secret lên git — chỉ set trên Railway)
VNPAY_TMN_CODE=CU183B14
VNPAY_HASH_SECRET=<secret từ email VNPAY>
VNPAY_PAY_URL=https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
VNPAY_RETURN_URL=https://sedsp-api-production.up.railway.app/api/v1/payments/vnpay-return
VNPAY_IPN_URL=https://sedsp-api-production.up.railway.app/api/v1/payments/vnpay-ipn

# Cloudinary — bắt buộc upload ảnh SP (lỗi "Unknown API key your_api_key" = thiếu biến)
CLOUDINARY_CLOUD_NAME=<cloud name>
CLOUDINARY_API_KEY=<api key>
CLOUDINARY_API_SECRET=<api secret>

# Local test IPN: VNPay/MoMo gọi server→server nên localhost không nhận được.
# Chạy `ngrok http 8080`, rồi set trong application-dev.yml:
#   app.payment.vnpay.ipn-url=https://xxxx.ngrok-free.dev/api/v1/payments/vnpay-ipn
# Railway đã public → không cần ngrok.
```

> **CORS:** Backend luôn merge thêm `https://*.vercel.app`. Nếu FE vẫn báo CORS, kiểm tra `FRONTEND_BASE_URL` đúng domain Vercel production (không dùng URL preview tạm).

## Vercel FE

Repo `love123334/smartecon-fe` — file `.env.production` đã trỏ Railway:

```env
VITE_USE_MOCK=false
VITE_API_BASE_URL=https://sedsp-api-production.up.railway.app/api/v1
VITE_BACKEND_ORIGIN=https://sedsp-api-production.up.railway.app
```

Sau khi đổi `VITE_*` trên Vercel Dashboard phải **Redeploy** (build lại).

**Flow thanh toán:** VNPay/MoMo return → FE `/cart?pay=success|failed|cancelled` + banner/toast.
