# LightGBM demand forecasting

Pipeline gộp dữ liệu các sản phẩm đủ điều kiện từ đơn `DELIVERED`, train một
Global LightGBM model và export tại `models/demand/global-demand.onnx`. Backend
dùng cùng model này với bộ feature lịch sử riêng của sản phẩm đang được chọn.

## Cài đặt

```powershell
python -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r ml\requirements.txt
```

PostgreSQL mặc định là `localhost:5432`, database `sedsp`, user `postgres`, mật
khẩu `123456`. Có thể cấu hình bằng `DATABASE_URL` hoặc các biến `PGHOST`,
`PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`.

## Train global model

```powershell
python ml\train_demand_models.py
```

Mặc định pipeline dùng mọi sản phẩm có ít nhất 60 ngày lịch sử. Có thể truyền
`--product-id` nhiều lần để giới hạn tập sản phẩm dùng trong một lần thử nghiệm.

Sau khi train, khởi động lại Backend. API trả `method = lightgbm_onnx` khi tìm
thấy global model. Nếu model thiếu hoặc lỗi, hệ thống dùng baseline và trả
`method = trend_blended_feature_forecast`.
