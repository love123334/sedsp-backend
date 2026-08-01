package com.example.secdsp.config;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Ultra-light probe for Railway (no Actuator group wiring). Prefer
 * /actuator/health/liveness; this is a fallback path if needed in service settings.
 */
@RestController
public class HealthzController {

    @GetMapping(value = "/healthz", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> healthz() {
        return Map.of("status", "UP");
    }
}
