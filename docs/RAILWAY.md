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
```

## Vercel FE

```env
VITE_USE_MOCK=false
VITE_API_BASE_URL=https://YOUR-APP.up.railway.app/api/v1
VITE_BACKEND_ORIGIN=https://YOUR-APP.up.railway.app
```

Sau khi đổi `VITE_*` phải **Redeploy** FE (build lại).
