package com.example.secdsp.modules.voucher.entity;

import com.example.secdsp.modules.product.entity.Product;
import com.example.secdsp.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "vouchers")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, length = 50)
    String code;

    @Column(nullable = false)
    String name;

    @Column(columnDefinition = "TEXT")
    String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "discount_type", nullable = false, columnDefinition = "voucher_discount_type")
    VoucherDiscountType discountType = VoucherDiscountType.PERCENTAGE;

    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    BigDecimal discountValue;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "voucher_scope")
    VoucherScope scope = VoucherScope.PLATFORM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    User seller;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "applies_to", nullable = false, columnDefinition = "voucher_applies_to")
    VoucherAppliesTo appliesTo = VoucherAppliesTo.ALL_PRODUCTS;

    @Column(name = "minimum_order_amount", nullable = false, precision = 12, scale = 2)
    BigDecimal minimumOrderAmount = BigDecimal.ZERO;

    @Column(name = "maximum_discount_amount", precision = 12, scale = 2)
    BigDecimal maximumDiscountAmount;

    @Column(name = "usage_limit")
    Integer usageLimit;

    @Column(name = "used_count", nullable = false)
    Integer usedCount = 0;

    @Column(name = "starts_at", nullable = false)
    OffsetDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    OffsetDateTime endsAt;

    @Column(name = "is_active", nullable = false)
    Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    User createdBy;

    @Column(name = "request_id")
    Long requestId;

    @ManyToMany
    @JoinTable(
        name = "voucher_products",
        joinColumns = @JoinColumn(name = "voucher_id"),
        inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    Set<Product> products = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (code != null) {
            code = code.trim().toUpperCase();
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
        if (code != null) {
            code = code.trim().toUpperCase();
        }
    }
}
