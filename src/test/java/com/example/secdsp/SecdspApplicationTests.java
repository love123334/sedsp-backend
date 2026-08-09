package com.example.secdsp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Smoke test — full {@code @SpringBootTest} needs PostgreSQL (Flyway migrations are PG-specific).
 * Docker/Railway build uses {@code bootJar -x test}; module tests cover business logic.
 */
class SecdspApplicationTests {

    @Test
    void applicationClassLoads() {
        assertNotNull(SecdspApplication.class);
    }
}
