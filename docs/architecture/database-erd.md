# Database ERD – Smart E-commerce DSSP

ERD này được tổng hợp từ các migration trong `src/main/resources/db/migration`. Các bảng phản ánh schema sau migration `V50`; các view reporting được liệt kê ở cuối tài liệu.

```mermaid
erDiagram
    roles ||--o{ users : has
    users ||--o{ products : sells
    categories ||--o{ products : classifies
    categories ||--o{ categories : parent_of

    users ||--o| carts : owns
    carts ||--o{ cart_items : contains
    products ||--o{ cart_items : added_to

    users ||--o{ orders : places
    customer_addresses ||--o{ orders : used_for
    vouchers o|--o{ orders : applied_to
    orders ||--o{ order_items : contains
    products ||--o{ order_items : purchased_as
    users ||--o{ order_items : fulfills
    orders ||--o| payments : paid_by
    orders ||--o{ order_tracking : tracked_by
    users ||--o{ order_tracking : updates

    products ||--o{ product_images : has
    products ||--o{ product_attributes : has
    products ||--o| inventory : stocked_in
    products ||--o{ inventory_logs : changes
    users ||--o{ inventory_logs : records
    products ||--o{ price_history : repriced
    users ||--o{ price_history : changes
    products ||--o{ product_reviews : reviewed
    users ||--o{ product_reviews : writes

    users ||--o{ customer_addresses : saves
    users ||--o| wishlists : owns
    wishlists ||--o{ wishlist_items : contains
    products ||--o{ wishlist_items : wished

    users ||--o{ vouchers : owns_or_creates
    vouchers ||--o{ voucher_products : targets
    products ||--o{ voucher_products : eligible
    users ||--o{ voucher_requests : submits
    users o|--o{ voucher_requests : reviews
    voucher_requests ||--o{ voucher_request_products : targets
    products ||--o{ voucher_request_products : requested_for
    vouchers o|--o{ voucher_requests : approved_as
    voucher_requests o|--o| vouchers : creates
    vouchers ||--o{ voucher_usages : used
    users ||--o{ voucher_usages : redeems
    orders o|--o{ voucher_usages : applied_in

    users ||--o{ customer_notifications : receives
    orders o|--o{ customer_notifications : triggers

    roles {
        bigint id PK
        varchar name UK
    }
    users {
        bigint id PK
        bigint role_id FK
        varchar username UK
        varchar email UK
        user_status status
    }
    categories {
        bigint id PK
        bigint parent_id FK
        varchar name
        varchar slug
    }
    products {
        bigint id PK
        bigint seller_id FK
        bigint category_id FK
        varchar name
        numeric price
        product_status status
    }
    carts { bigint id PK; bigint user_id FK UK }
    cart_items { bigint id PK; bigint cart_id FK; bigint product_id FK; integer quantity }
    orders { bigint id PK; bigint user_id FK; bigint address_id FK; bigint voucher_id FK; numeric total_amount; order_status status }
    order_items { bigint id PK; bigint order_id FK; bigint product_id FK; bigint seller_id FK; integer quantity; numeric subtotal }
    payments { bigint id PK; bigint order_id FK UK; payment_method_enum payment_method; payment_status status; numeric amount }
    order_tracking { bigint id PK; bigint order_id FK; bigint updated_by FK; order_tracking_event event }
    customer_addresses { bigint id PK; bigint user_id FK; varchar receiver_name; text address_line }
    product_images { bigint id PK; bigint product_id FK; text image_url; boolean is_primary }
    product_attributes { bigint id PK; bigint product_id FK; varchar attribute_name; varchar attribute_value }
    inventory { bigint id PK; bigint product_id FK UK; integer available_quantity; integer reserved_quantity }
    inventory_logs { bigint id PK; bigint product_id FK; bigint updated_by FK; integer change_amount }
    price_history { bigint id PK; bigint product_id FK; bigint changed_by FK; numeric old_price; numeric new_price }
    product_reviews { bigint id PK; bigint product_id FK; bigint user_id FK; integer rating }
    wishlists { bigint id PK; bigint user_id FK UK }
    wishlist_items { bigint id PK; bigint wishlist_id FK; bigint product_id FK }
    vouchers { bigint id PK; bigint seller_id FK; bigint created_by FK; bigint request_id FK; voucher_scope scope; varchar code }
    voucher_products { bigint voucher_id PK,FK; bigint product_id PK,FK }
    voucher_requests { bigint id PK; bigint seller_id FK; bigint reviewed_by FK; bigint voucher_id FK }
    voucher_request_products { bigint request_id PK,FK; bigint product_id PK,FK }
    voucher_usages { bigint id PK; bigint voucher_id FK; bigint user_id FK; bigint order_id FK }
    customer_notifications { bigint id PK; bigint user_id FK; bigint order_id FK; boolean is_read }
    email_otps { bigint id PK; varchar email; varchar otp; timestamp expiry_time; boolean verified }
```

## Reporting views

Các view dưới đây nằm trong schema `reporting`, phục vụ dashboard/phân tích và không phải bảng vật lý:

- `platform_daily_revenue`
- `platform_order_status_daily`
- `platform_payment_method_daily`
- `platform_seller_daily`
- `platform_product_daily`
- `platform_category_daily`
- `platform_activity_daily`
- `platform_current_summary`
- `platform_customer_purchase_daily`

Các view reporting lấy dữ liệu chủ yếu từ `orders`, `order_items`, `payments`, `products`, `categories`, `users` và `customer_addresses`. Chi tiết cột và logic truy vấn được định nghĩa trong `V37__create_platform_reporting_views.sql` và `V38__create_customer_purchase_reporting_view.sql`.

Migration `V53__remove_legacy_dss_modules.sql` loại bỏ các bảng của dự báo nhu cầu lưu lịch sử, kịch bản DSS và What-if cũ. Các view trong schema `reporting` không phụ thuộc những bảng này.

## Quy ước

- `PK`: primary key; `FK`: foreign key; `UK`: unique constraint/index.
- Ký hiệu `||--o{` là quan hệ một-nhiều; `||--o|` là một-một tùy chọn; `o|--o{` là quan hệ tùy chọn một-nhiều.
- `email_otps` không có foreign key tới `users`; liên kết nghiệp vụ được thực hiện bằng email.
- Các trường `deleted_at` là soft-delete và được lược giản khỏi sơ đồ để ERD dễ đọc.
- Các kiểu enum (`user_status`, `product_status`, `order_status`, payment/recommendation/voucher enums) được giữ dưới dạng kiểu cột trong entity tương ứng.
