package com.example.secdsp.modules.user.repository;

import com.example.secdsp.modules.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @EntityGraph(attributePaths = {
        "customer",
        "seller",
        "manager",
        "admin"
    })
    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    @EntityGraph(attributePaths = {
        "customer",
        "seller",
        "manager",
        "admin"
    })
    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByPhoneAndIdNot(String phone, Long id);

    @Query("""
            SELECT u FROM User u
            WHERE u.deletedAt IS NULL
            AND (:keyword IS NULL OR :keyword = '' OR
                 LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                 LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                 LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    @EntityGraph(attributePaths = {
        "customer",
        "seller",
        "manager",
        "admin"
    })
    Page<User> searchUsers(
        @Param("keyword") String keyword,
        Pageable pageable
    );
}