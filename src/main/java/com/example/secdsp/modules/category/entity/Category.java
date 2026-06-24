package com.example.secdsp.modules.category.entity;

import com.example.secdsp.modules.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.util.List;

@Entity
@Table(name = "categories")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@SQLDelete(
    sql = """
            UPDATE categories
            SET deleted_at = CURRENT_TIMESTAMP
            WHERE id = ?
        """
)
@SQLRestriction("deleted_at IS NULL")
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, length = 150)
    String name;

    @Column(nullable = false, unique = true, length = 150)
    String slug;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    Category parent;

    @OneToMany(
        mappedBy = "parent",
        fetch = FetchType.LAZY
    )
    List<Category> children;
}
