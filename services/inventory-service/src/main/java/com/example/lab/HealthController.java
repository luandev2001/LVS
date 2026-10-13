package com.example.lab;

import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    @GetMapping("/internal/bootstrap")
    Map<String, Object> bootstrap() {
        return Map.of(
            "service", "inventory-service",
            "phase", "PHASE-0",
            "status", "UP",
            "timestamp", Instant.now().toString()
        );
    }
}
