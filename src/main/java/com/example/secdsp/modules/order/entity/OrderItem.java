package com.example.secdsp.modules.order.entity;

import com.example.secdsp.modules.product.entity.Product;
import com.example.secdsp.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    User seller;

    @Column(name = "product_name_at_purchase", nullable = false, length = 255)
    String productNameAtPurchase;

    @Column(name = "quantity", nullable = false)
    Integer quantity;

    @Column(name = "unit_price_at_purchase", nullable = false, precision = 12, scale = 2)
    BigDecimal unitPriceAtPurchase;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    BigDecimal subtotal;
}