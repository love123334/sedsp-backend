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

# Gmail OTP (cùng tài khoản đang dùng local) — App Password, không phải mật khẩu Gmail thường
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=sedsp.official@gmail.com
MAIL_PASSWORD=<gmail-app-password>
MAIL_FROM=sedsp.official@gmail.com

# Nếu log báo "Couldn't connect to host smtp.gmail.com:587" (Railway block SMTP):
# thử SSL 465:
#   MAIL_PORT=465
#   MAIL_SMTP_SSL=true
#   MAIL_SMTP_STARTTLS=false
# hoặc SMTP qua Brevo/SendGrid/Resend (HTTPS API ổn định hơn Gmail từ cloud).
```

## Vercel FE

```env
VITE_USE_MOCK=false
VITE_API_BASE_URL=https://YOUR-APP.up.railway.app/api/v1
VITE_BACKEND_ORIGIN=https://YOUR-APP.up.railway.app
```

Sau khi đổi `VITE_*` phải **Redeploy** FE (build lại).
