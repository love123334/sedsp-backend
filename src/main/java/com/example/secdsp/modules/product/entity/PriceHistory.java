package com.example.secdsp.modules.product.entity;

import com.example.secdsp.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "price_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    Product product;

    @Column(name = "old_price", precision = 12, scale = 2)
    BigDecimal oldPrice;

    @Column(name = "new_price", precision = 12, scale = 2)
    BigDecimal newPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    User changedBy;

    @Column(name = "changed_at", nullable = false, updatable = false)
    OffsetDateTime changedAt;

    @PrePersist
    protected void onCreate() {
        this.changedAt = OffsetDateTime.now();
    }
}