package com.cloudkitchen.rbac.controller;

import com.cloudkitchen.rbac.dto.auth.RegisterRequest;
import com.cloudkitchen.rbac.security.JwtTokenProvider;
import com.cloudkitchen.rbac.service.UserService;
import com.cloudkitchen.rbac.util.ResponseBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;
    private final JwtTokenProvider jwt;

    public UserController(UserService userService, JwtTokenProvider jwt) {
        this.userService = userService;
        this.jwt = jwt;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestHeader(value = "Authorization", required = true) String authHeader,
            @RequestParam(value = "page", defaultValue = "0") @Min(0) Integer page,
            @RequestParam(value = "size", defaultValue = "20") @Min(1) Integer size) {
        try {
            if (!isValidAuthHeader(authHeader)) {
                logger.warn("Invalid authorization header format");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ResponseBuilder.error(401, "Invalid authorization header"));
            }
            
            Integer userId = extractUserId(authHeader);
            logger.info("User {} requesting all users, page: {}, size: {}", userId, page, size);
            
            return ResponseEntity.ok(ResponseBuilder.success(200, "Users retrieved successfully", 
                    userService.getAllUsers(userId, page, size)));
        } catch (IllegalArgumentException e) {
            logger.warn("Authentication failed for getAllUsers: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseBuilder.error(401, "Authentication failed"));
        } catch (Exception e) {
            logger.error("Unexpected error in getAllUsers: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(500, "Internal server error"));
        }
    }

    @GetMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> getUserById(
            @PathVariable @NotNull @Min(1) Integer userId, 
            @RequestHeader(value = "Authorization", required = true) String authHeader) {
        try {
            if (!isValidAuthHeader(authHeader)) {
                logger.warn("Invalid authorization header format for getUserById");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ResponseBuilder.error(401, "Invalid authorization header"));
            }
            
            Integer requestingUserId = extractUserId(authHeader);
            logger.info("User {} requesting details for user {}", requestingUserId, userId);
            
            return ResponseEntity.ok(ResponseBuilder.success(200, "User retrieved successfully", 
                    userService.getUserById(userId, requestingUserId)));
        } catch (IllegalArgumentException e) {
            logger.warn("Authentication failed for getUserById: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseBuilder.error(401, "Authentication failed"));
        } catch (Exception e) {
            logger.error("Unexpected error in getUserById for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(500, "Internal server error"));
        }
    }
    
    @PostMapping
    @PreAuthorize("hasRole('super_admin') or hasRole('merchant_admin')")
    public ResponseEntity<Map<String, Object>> createUser(
            @Valid @RequestBody RegisterRequest req, 
            @RequestHeader(value = "Authorization", required = true) String authHeader) {
        try {
            if (!isValidAuthHeader(authHeader)) {
                logger.warn("Invalid authorization header format for createUser");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ResponseBuilder.error(401, "Invalid authorization header"));
            }
            
            Integer requestingUserId = extractUserId(authHeader);
            String maskedPhone = maskPhoneNumber(req.getPhone());
            logger.info("User {} creating new user with phone: {}", requestingUserId, maskedPhone);
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ResponseBuilder.success(201, "User created successfully", 
                            userService.createUser(req, requestingUserId)));
        } catch (IllegalArgumentException e) {
            logger.warn("Authentication failed for createUser: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseBuilder.error(401, "Authentication failed"));
        } catch (Exception e) {
            logger.error("Unexpected error in createUser: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(500, "Internal server error"));
        }
    }
    
    @PutMapping("/{userId}")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable @NotNull @Min(1) Integer userId, 
            @Valid @RequestBody RegisterRequest req, 
            @RequestHeader(value = "Authorization", required = true) String authHeader) {
        try {
            if (!isValidAuthHeader(authHeader)) {
                logger.warn("Invalid authorization header format for updateUser");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ResponseBuilder.error(401, "Invalid authorization header"));
            }
            
            Integer requestingUserId = extractUserId(authHeader);
            logger.info("User {} updating user {}", requestingUserId, userId);
            
            return ResponseEntity.ok(ResponseBuilder.success(200, "User updated successfully", 
                    userService.updateUser(userId, req, requestingUserId)));
        } catch (IllegalArgumentException e) {
            logger.warn("Authentication failed for updateUser: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseBuilder.error(401, "Authentication failed"));
        } catch (Exception e) {
            logger.error("Unexpected error in updateUser for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(500, "Internal server error"));
        }
    }
    
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('super_admin')")
    public ResponseEntity<Map<String, Object>> deleteUser(
            @PathVariable @NotNull @Min(1) Integer userId, 
            @RequestHeader(value = "Authorization", required = true) String authHeader) {
        try {
            if (!isValidAuthHeader(authHeader)) {
                logger.warn("Invalid authorization header format for deleteUser");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ResponseBuilder.error(401, "Invalid authorization header"));
            }
            
            Integer requestingUserId = extractUserId(authHeader);
            logger.warn("User {} attempting to delete user {}", requestingUserId, userId);
            
            userService.deleteUser(userId, requestingUserId);
            logger.info("User {} successfully deleted by user {}", userId, requestingUserId);
            
            return ResponseEntity.ok(ResponseBuilder.success(200, "User deleted successfully"));
        } catch (IllegalArgumentException e) {
            logger.warn("Authentication failed for deleteUser: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ResponseBuilder.error(401, "Authentication failed"));
        } catch (Exception e) {
            logger.error("Unexpected error in deleteUser for user {}: {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(500, "Internal server error"));
        }
    }
    
    private boolean isValidAuthHeader(String authHeader) {
        return authHeader != null && authHeader.startsWith("Bearer ") && authHeader.length() > 7;
    }
    
    private Integer extractUserId(String authHeader) {
        if (!isValidAuthHeader(authHeader)) {
            throw new IllegalArgumentException("Invalid authorization header format");
        }
        
        try {
            String token = authHeader.substring(7).trim();
            if (token.isEmpty()) {
                throw new IllegalArgumentException("Empty token");
            }
            
            if (!jwt.validateAccessToken(token)) {
                throw new IllegalArgumentException("Invalid or expired token");
            }
            
            return jwt.getUserIdFromToken(token);
        } catch (Exception e) {
            logger.debug("Token extraction failed: {}", e.getMessage());
            throw new IllegalArgumentException("Token validation failed", e);
        }
    }
    
    private String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return "****" + phone.substring(phone.length() - 4);
    }
}