# Deploy SEDSP Backend lên Render + nối Vercel

## A. Repo / nhánh

- Repo: **`love123334/sedsp-backend`**
- Branch: **`render`**
- Có sẵn: `Dockerfile`, `docker-entrypoint.sh`, `application-render.yml`, `render.yaml`

## B. Tạo Blueprint trên Render

1. https://dashboard.render.com → **New** → **Blueprint**
2. Connect repo **`love123334/sedsp-backend`**
3. Branch: **`render`**
4. Render đọc `render.yaml` → tạo:
   - Postgres `sedsp-db` (free)
   - Redis/Key Value `sedsp-redis` (free)
   - Web `sedsp-api` (Docker)
5. **Apply** và đợi build (~5–10 phút lần đầu)

## C. Env bổ sung (Dashboard → sedsp-api → Environment)

Đã auto: `DATABASE_URL`, `REDIS_URL`, `JWT_SECRET`, `CORS_ALLOWED_ORIGINS`.

Thêm nếu dùng OTP / upload ảnh:

| Key | Value |
|-----|--------|
| `MAIL_USERNAME` | Gmail |
| `MAIL_PASSWORD` | App password |
| `MAIL_FROM` | cùng Gmail |
| `CLOUDINARY_CLOUD_NAME` | … |
| `CLOUDINARY_API_KEY` | … |
| `CLOUDINARY_API_SECRET` | … |

URL dạng: `https://sedsp-api.onrender.com`  
Health: `https://sedsp-api.onrender.com/actuator/health` → `{"status":"UP"}`

Deploy log kỳ vọng: `[entrypoint] Datasource configured (sslmode=require) …`

## D. Vercel (FE)

```env
VITE_USE_MOCK=false
VITE_API_BASE_URL=https://sedsp-api.onrender.com/api/v1
VITE_BACKEND_ORIGIN=https://sedsp-api.onrender.com
```

## E. Free tier

- Web **sleep** ~15 phút không traffic → request đầu chậm 30–60s
- Postgres free có giới hạn

## F. Manual (không Blueprint)

1. New PostgreSQL + Key Value  
2. New Web Service → Docker → branch **`render`**  
3. Env:
   - `SPRING_PROFILES_ACTIVE=render`
   - `DATABASE_URL` = **Internal** Database URL (từ Postgres)
   - `REDIS_URL` = Redis connection string
   - `CORS_ALLOWED_ORIGINS=https://YOUR-VERCEL.vercel.app,https://*.vercel.app`
   - `JWT_SECRET` = chuỗi dài ≥32 ký tự
