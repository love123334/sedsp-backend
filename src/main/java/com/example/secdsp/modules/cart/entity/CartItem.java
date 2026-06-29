package com.example.secdsp.modules.cart.entity;

import com.example.secdsp.modules.common.entity.BaseEntity;
import com.example.secdsp.modules.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(
    name = "cart_items",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_cart_product",
            columnNames = {"cart_id", "product_id"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SQLDelete(
    sql = """
        UPDATE cart_items
        SET deleted_at = CURRENT_TIMESTAMP
        WHERE id = ?
    """
)
@SQLRestriction("deleted_at IS NULL")
public class CartItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    Product product;

    @Column(nullable = false)
    Integer quantity;
}