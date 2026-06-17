package com.example.secdsp.modules.product.entity;

import com.example.secdsp.modules.brand.entity.Brand;
import com.example.secdsp.modules.category.entity.Category;
import com.example.secdsp.modules.common.entity.BaseEntity;
import com.example.secdsp.modules.seller.entity.Seller;
import com.example.secdsp.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
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
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    Seller seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    Brand brand;

    @Column(nullable = false, length = 255)
    String name;

    @Column(unique = true, length = 255)
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
