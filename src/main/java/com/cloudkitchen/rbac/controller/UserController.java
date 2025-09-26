package com.cloudkitchen.rbac.controller;

import com.cloudkitchen.rbac.dto.auth.RegisterRequest;
import com.cloudkitchen.rbac.security.JwtTokenProvider;
import com.cloudkitchen.rbac.service.UserService;
import com.cloudkitchen.rbac.util.ResponseBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final JwtTokenProvider jwt;

    public UserController(UserService userService, JwtTokenProvider jwt) {
        this.userService = userService;
        this.jwt = jwt;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllUsers(@RequestHeader("Authorization") String authHeader) {
        try {
            Integer userId = extractUserId(authHeader);
            return ResponseEntity.ok(ResponseBuilder.success(200, "Users retrieved successfully", userService.getAllUsers(userId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(ResponseBuilder.error(401, "Invalid token"));
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable Integer userId, @RequestHeader("Authorization") String authHeader) {
        try {
            Integer requestingUserId = extractUserId(authHeader);
            return ResponseEntity.ok(ResponseBuilder.success(200, "User retrieved successfully", userService.getUserById(userId, requestingUserId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(ResponseBuilder.error(401, "Invalid token"));
        }
    }
    
    @PostMapping
    public ResponseEntity<Map<String, Object>> createUser(@Valid @RequestBody RegisterRequest req, @RequestHeader("Authorization") String authHeader) {
        try {
            Integer requestingUserId = extractUserId(authHeader);
            return ResponseEntity.ok(ResponseBuilder.success(201, "User created successfully", userService.createUser(req, requestingUserId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(ResponseBuilder.error(401, "Invalid token"));
        }
    }
    
    @PutMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable Integer userId, @Valid @RequestBody RegisterRequest req, @RequestHeader("Authorization") String authHeader) {
        try {
            Integer requestingUserId = extractUserId(authHeader);
            return ResponseEntity.ok(ResponseBuilder.success(200, "User updated successfully", userService.updateUser(userId, req, requestingUserId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(ResponseBuilder.error(401, "Invalid token"));
        }
    }
    
    @DeleteMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> deleteUser(@PathVariable Integer userId, @RequestHeader("Authorization") String authHeader) {
        try {
            Integer requestingUserId = extractUserId(authHeader);
            userService.deleteUser(userId, requestingUserId);
            return ResponseEntity.ok(ResponseBuilder.success(200, "User deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).body(ResponseBuilder.error(401, "Invalid token"));
        }
    }
    
    private Integer extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid authorization header");
        }
        try {
            return jwt.getUserIdFromToken(authHeader.replace("Bearer ", ""));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid token");
        }
    }
}