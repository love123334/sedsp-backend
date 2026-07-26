# Deploy SEDSP Backend trên Railway

## Khác lần trước

- **Không** dùng shell entrypoint `exit 1` (trước đó healthcheck fail vì Java không kịp start)
- Java tự map `DATABASE_PUBLIC_URL` / `DATABASE_URL` / `PG*` → JDBC
- Dockerfile chỉ chạy `java -jar`

## A. Railway setup

1. New Project → Deploy from GitHub → `love123334/sedsp-backend` → branch **`railway`** (hoặc `main` sau khi merge)
2. Thêm **PostgreSQL** (vd. `sedsp-db`) trong cùng project
3. (Khuyến nghị) thêm **Redis**

## B. Variables trên service API (`sedsp-api`)

**Bắt buộc — Add Variable → Reference từ Postgres:**

| Tên trên API | Lấy từ sedsp-db | Ghi chú |
|--------------|-----------------|--------|
| `DATABASE_PUBLIC_URL` | `DATABASE_PUBLIC_URL` | **Ưu tiên** — luôn có host public |
| `PGHOST` | `PGHOST` | backup |
| `PGPORT` | `PGPORT` | backup |
| `PGUSER` | `PGUSER` | backup |
| `PGPASSWORD` | `PGPASSWORD` | backup |
| `PGDATABASE` | `PGDATABASE` | backup |

Có thể thêm luôn `DATABASE_URL` (private). Nếu private networking tắt, private URL có thể **empty** — vì vậy cần `DATABASE_PUBLIC_URL`.

**Redis (nếu có):**

| Tên | Reference |
|-----|-----------|
| `REDIS_URL` | Redis → `REDIS_URL` hoặc `REDIS_PUBLIC_URL` |

**Tự thêm:**

```env
SPRING_PROFILES_ACTIVE=prod
JAVA_OPTS=-Xms256m -Xmx512m -Duser.timezone=Asia/Ho_Chi_Minh
JWT_SECRET=<chuỗi ≥32 ký tự>
CORS_ALLOWED_ORIGINS=https://YOUR-VERCEL.vercel.app,https://*.vercel.app,http://localhost:5173
```

## C. Settings

- **Networking** → Generate Domain  
- **Deploy → Custom Start Command**: để **trống** (dùng Dockerfile ENTRYPOINT)
- Root directory: repo backend (Dockerfile ở root)

## D. Kiểm tra

Deploy Logs phải có:

```
[datasource] Using DATABASE_PUBLIC_URL (len=…)
[datasource] spring.datasource.url=jdbc:postgresql://***@…
```

Health: `https://YOUR-APP.up.railway.app/actuator/health` → `{"status":"UP"}`

## E. Vercel FE

```env
VITE_USE_MOCK=false
VITE_API_BASE_URL=https://YOUR-APP.up.railway.app/api/v1
VITE_BACKEND_ORIGIN=https://YOUR-APP.up.railway.app
```
