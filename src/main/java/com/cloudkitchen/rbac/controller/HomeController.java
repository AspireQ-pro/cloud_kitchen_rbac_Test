package com.cloudkitchen.rbac.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, String> home() {
        Map<String, String> response = new HashMap<>();
        response.put("message", "✅ Cloud Kitchen RBAC Service is running!");
        response.put("swaggerDocs", "http://localhost:9090/swagger-ui/index.html");
        return response;
    }
}
