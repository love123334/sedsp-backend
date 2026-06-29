package com.example.secdsp.modules.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    com.example.secdsp.modules.product.entity.Product product;

    @Column(name = "available_quantity", nullable = false)
    Integer availableQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    Integer reservedQuantity;

    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;
}