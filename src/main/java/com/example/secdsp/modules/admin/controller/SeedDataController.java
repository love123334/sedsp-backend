package com.example.secdsp.modules.admin.controller;

import com.example.secdsp.common.exception.UnauthorizedException;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.user.entity.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
public class SeedDataController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/seed-data/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getSeedDataStatus() {
        Map<String, Object> status = new HashMap<>();
        
        try {
            Integer productCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM products WHERE deleted_at IS NULL", Integer.class
            );
            Integer categoryCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM categories WHERE deleted_at IS NULL", Integer.class
            );
            Integer userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE deleted_at IS NULL", Integer.class
            );
            
            status.put("hasSeedData", productCount != null && productCount > 0);
            status.put("productCount", productCount != null ? productCount : 0);
            status.put("categoryCount", categoryCount != null ? categoryCount : 0);
            status.put("userCount", userCount != null ? userCount : 0);
            status.put("status", "success");
            
        } catch (Exception e) {
            log.error("Error checking seed data status", e);
            status.put("status", "error");
            status.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(status);
    }

    @GetMapping("/seed-data/download")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Resource> downloadSeedData() {
        try {
            Resource resource = new ClassPathResource("db/seed/sedsp_seed.sql");
            
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN);
            headers.setContentDispositionFormData("attachment", "sedsp_seed.sql");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
                
        } catch (Exception e) {
            log.error("Error downloading seed data", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/seed-data/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> importSeedData() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (!SecurityUtils.hasRole(UserRole.ADMIN)) {
                throw new UnauthorizedException("Only admin can import seed data");
            }
            
            Resource resource = new ClassPathResource("db/seed/sedsp_seed.sql");
            
            if (!resource.exists()) {
                result.put("status", "error");
                result.put("message", "Seed file not found at db/seed/sedsp_seed.sql");
                return ResponseEntity.ok(result);
            }
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                
                StringBuilder sql = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sql.append(line).append("\n");
                }
                
                jdbcTemplate.execute(sql.toString());
                
                result.put("status", "success");
                result.put("message", "Seed data imported successfully");
                log.info("Seed data imported successfully by admin");
                
            }
            
        } catch (Exception e) {
            log.error("Error importing seed data", e);
            result.put("status", "error");
            result.put("message", "Failed to import seed data: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
}
