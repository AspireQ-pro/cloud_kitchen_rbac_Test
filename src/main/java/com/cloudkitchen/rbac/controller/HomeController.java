package com.cloudkitchen.rbac.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, String> home() {
        return Map.of(
            "message", "✅ Cloud Kitchen RBAC Service is running!",
            "swaggerDocs", "/swagger-ui/index.html"
        );
    }
}
