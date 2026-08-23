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

    @EntityGraph(attributePaths = "role")
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = "role")
    Optional<User> findById(Long id);

    boolean existsByPhoneAndIdNot(String phone, Long id);

    /**
     * Explicit countQuery — {@code @EntityGraph} + Page otherwise breaks count SQL
     * (admin /users returned 400 DataAccessException on Railway).
     */
    @Query(
        value = """
            SELECT u
            FROM User u
            WHERE :keyword = ''
               OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR (u.fullName IS NOT NULL
                   AND LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """,
        countQuery = """
            SELECT COUNT(u)
            FROM User u
            WHERE :keyword = ''
               OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR (u.fullName IS NOT NULL
                   AND LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """
    )
    @EntityGraph(attributePaths = "role")
    Page<User> searchUsers(
        @Param("keyword") String keyword,
        Pageable pageable
    );

    boolean existsByEmail(String email);

    @Query("""
        SELECT COUNT(u)
        FROM User u
        JOIN u.role r
        WHERE r.name = :roleName
        """)
    long countByRoleName(@Param("roleName") String roleName);
}
