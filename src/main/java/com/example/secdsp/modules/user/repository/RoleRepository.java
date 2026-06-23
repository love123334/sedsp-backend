package com.example.secdsp.modules.user.repository;

import com.example.secdsp.modules.user.entity.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    @EntityGraph(attributePaths = {})
    Optional<Role> findByName(String name);
}
