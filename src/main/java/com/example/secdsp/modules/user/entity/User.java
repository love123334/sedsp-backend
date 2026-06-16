package com.example.secdsp.modules.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "full_name", length = 150)
    private String fullName;

    @Column(unique = true, length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(
        name = "status",
        nullable = false,
        columnDefinition = "user_status"
    )
    private UserStatus status = UserStatus.ACTIVE;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private Customer customer;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private Seller seller;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private Manager manager;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private Admin admin;

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

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
