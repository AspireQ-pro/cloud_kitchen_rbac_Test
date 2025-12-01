package com.cloudkitchen.rbac.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cloudkitchen.rbac.service.AuthService;
import com.cloudkitchen.rbac.util.ResponseBuilder;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {
    private final AuthService authService;

    public HealthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> getUserCount() {
        long count = authService.getUserCount();
        return ResponseEntity.ok(ResponseBuilder.success(200, "User count", Map.of("userCount", count)));
    }
}