package com.cloudkitchen.rbac.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "message", "Application is running",
            "timestamp", System.currentTimeMillis()
        ));
    }

    @GetMapping("/cors")
    public ResponseEntity<Map<String, String>> testCors() {
        return ResponseEntity.ok(Map.of(
            "message", "CORS is working",
            "server", "EC2"
        ));
    }

    @GetMapping("/db")
    public ResponseEntity<Map<String, Object>> testDatabase() {
        try {
            // This will be injected if needed for testing
            return ResponseEntity.ok(Map.of(
                "status", "Database connection test endpoint",
                "message", "Use /api/auth/login to test actual database operations"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "status", "ERROR",
                "message", e.getMessage()
            ));
        }
    }
}