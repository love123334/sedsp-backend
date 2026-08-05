package com.example.secdsp.modules.product.entity;

import com.example.secdsp.modules.category.entity.Category;
import com.example.secdsp.modules.common.entity.BaseEntity;
import com.example.secdsp.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SQLDelete(sql = "UPDATE products SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    Category category;

    @Column(nullable = false, length = 255)
    String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    User seller;

    @Column(nullable = false, length = 255)
    String slug;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal price;

    @Column(name = "cost_price", precision = 12, scale = 2)
    BigDecimal costPrice;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(
        name = "status",
        nullable = false,
        columnDefinition = "product_status"
    )
    ProductStatus status = ProductStatus.ACTIVE;

    @OneToMany(
        mappedBy = "product",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    List<ProductImage> productImages = new ArrayList<>();

    @OneToMany(
        mappedBy = "product",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    List<ProductAttribute> productAttributes = new ArrayList<>();
}