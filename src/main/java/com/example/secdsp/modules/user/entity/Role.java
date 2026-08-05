package com.example.secdsp.modules.user.entity;

import com.example.secdsp.modules.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@NoArgsConstructor
@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "roles")
@SQLDelete(sql = "UPDATE roles SET deleted_at = now() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class Role extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, unique = true, length = 50)
    String name;

    @Column(columnDefinition = "TEXT")
    String description;
}