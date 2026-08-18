package com.example.secdsp.modules.dss.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "advanced_price_sessions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdvancedPriceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "seller_id", nullable = false)
    Long sellerId;

    @Column(name = "product_id", nullable = false)
    Long productId;

    @Column(name = "product_name", nullable = false)
    String productName;

    @Column(name = "from_date", nullable = false)
    LocalDate fromDate;

    @Column(name = "to_date", nullable = false)
    LocalDate toDate;

    @Column(name = "forecast_period", nullable = false)
    Integer forecastPeriod;

    @Column(name = "estimated_order_cost", nullable = false, precision = 14, scale = 2)
    BigDecimal estimatedOrderCost;

    @Column(name = "base_price", nullable = false, precision = 14, scale = 2)
    BigDecimal basePrice;

    @Column(name = "cost_price", nullable = false, precision = 14, scale = 2)
    BigDecimal costPrice;

    @Column(name = "historical_quantity_sold", nullable = false)
    Long historicalQuantitySold;

    @Column(name = "average_elasticity", nullable = false, precision = 12, scale = 6)
    BigDecimal averageElasticity;

    @Column(name = "elasticity_source", nullable = false, length = 40)
    String elasticitySource;

    @Column(name = "baseline_forecast_demand", nullable = false)
    Long baselineForecastDemand;

    @Column(name = "forecast_method", nullable = false, length = 100)
    String forecastMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    AdvancedPriceSessionStatus status;

    @Column(name = "applied_at")
    OffsetDateTime appliedAt;

    @Version
    @Column(nullable = false)
    Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = AdvancedPriceSessionStatus.ACTIVE;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
