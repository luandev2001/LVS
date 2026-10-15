package com.example.lab.inventory.controller;

import com.example.lab.inventory.service.ResilienceLabService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/resilience-lab")
public class ResilienceLabController {
    private final ResilienceLabService service;

    public ResilienceLabController(ResilienceLabService service) {
        this.service = service;
    }

    @GetMapping("/dependency")
    public ResilienceLabService.Result call(@RequestParam(defaultValue = "false") boolean fail,
                                            @RequestParam(defaultValue = "0") long delayMs,
                                            @RequestParam(defaultValue = "0") int retries) {
        return service.call(fail, delayMs, retries);
    }

    @ExceptionHandler(ResilienceLabService.CircuitOpenException.class)
    public ResponseEntity<Map<String, String>> circuitOpen(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", "CIRCUIT_OPEN", "message", ex.getMessage()));
    }

    @ExceptionHandler(ResilienceLabService.BulkheadFullException.class)
    public ResponseEntity<Map<String, String>> bulkheadFull(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("error", "BULKHEAD_FULL", "message", ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> dependencyFailed(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", "DEPENDENCY_FAILURE", "message", ex.getMessage()));
    }
}
