package com.example.secdsp.modules.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InventoryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    com.example.secdsp.modules.product.entity.Product product;

    @Column(name = "change_amount", nullable = false)
    Integer changeAmount;

    @Column(name = "previous_quantity", nullable = false)
    Integer previousQuantity;

    @Column(name = "current_quantity", nullable = false)
    Integer currentQuantity;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "reason", nullable = false, columnDefinition = "inventory_log_reason")
    InventoryLogReason reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", nullable = false)
    com.example.secdsp.modules.user.entity.User updatedBy;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}