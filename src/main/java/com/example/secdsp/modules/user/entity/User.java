package com.example.secdsp.modules.user.entity;

import com.example.secdsp.modules.common.entity.BaseEntity;
import com.example.secdsp.modules.seller.entity.Seller;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, unique = true, length = 100)
    String username;

    @Column(nullable = false, unique = true, length = 150)
    String email;

    @Column(nullable = false)
    String password;

    @Column(name = "full_name", length = 150)
    String fullName;

    @Column(unique = true, length = 20)
    String phone;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(
        name = "status",
        nullable = false,
        columnDefinition = "user_status"
    )
    UserStatus status = UserStatus.ACTIVE;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    Customer customer;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    Seller seller;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    Manager manager;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    Admin admin;

    @Transient
    public UserRole getRole() {

        int count = 0;

        if (admin != null) count++;
        if (manager != null) count++;
        if (seller != null) count++;
        if (customer != null) count++;

        if (count > 1) {
            throw new IllegalStateException(
                "User " + id + " has multiple roles"
            );
        }

        if (admin != null) return UserRole.ADMIN;
        if (manager != null) return UserRole.MANAGER;
        if (seller != null) return UserRole.SELLER;
        if (customer != null) return UserRole.CUSTOMER;

        return null;
    }
}
