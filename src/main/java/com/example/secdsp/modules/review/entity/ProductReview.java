package com.example.secdsp.modules.review.entity;

import com.example.secdsp.modules.common.entity.BaseEntity;
import com.example.secdsp.modules.product.entity.Product;
import com.example.secdsp.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "product_reviews")
@Getter
@Setter
@NoArgsConstructor
@SQLDelete(sql = """
    UPDATE product_reviews
    SET deleted_at = CURRENT_TIMESTAMP
    WHERE id = ?
""")
@SQLRestriction("deleted_at IS NULL")
public class ProductReview extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;
}