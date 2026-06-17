package com.example.secdsp.modules.brand.entity;

import com.example.secdsp.modules.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "brands")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Brand extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, unique = true, length = 150)
    String name;

    @Column(nullable = false, unique = true, length = 150)
    String slug;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "logo_url", columnDefinition = "TEXT")
    String logoUrl;
}