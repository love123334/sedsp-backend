package com.example.secdsp.modules.dss.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "advanced_price_scenarios")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdvancedPriceScenario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "session_id", nullable = false)
    Long sessionId;

    @Column(name = "price_change_percent", nullable = false, precision = 6, scale = 2)
    BigDecimal priceChangePercent;

    @Column(name = "new_price", nullable = false, precision = 14, scale = 2)
    BigDecimal newPrice;

    @Column(name = "profit_per_product", nullable = false, precision = 14, scale = 2)
    BigDecimal profitPerProduct;

    @Column(name = "demand_multiplier", nullable = false, precision = 14, scale = 6)
    BigDecimal demandMultiplier;

    @Column(name = "forecast_demand", nullable = false)
    Long forecastDemand;

    @Column(name = "expected_profit", nullable = false, precision = 18, scale = 2)
    BigDecimal expectedProfit;

    @Column(name = "applied_at")
    OffsetDateTime appliedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    OffsetDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}

