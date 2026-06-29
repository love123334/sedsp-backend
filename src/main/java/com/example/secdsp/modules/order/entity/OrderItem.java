package com.example.secdsp.modules.order.entity;

import com.example.secdsp.modules.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    Product product;

    @Column(name = "product_name_at_purchase", nullable = false)
    String productNameAtPurchase;

    @Column(name = "quantity", nullable = false)
    Integer quantity;

    @Column(name = "unit_price_at_purchase", nullable = false)
    BigDecimal unitPriceAtPurchase;

    @Column(name = "subtotal", nullable = false)
    BigDecimal subtotal;
}