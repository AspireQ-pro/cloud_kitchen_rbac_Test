package com.cloudkitchen.rbac.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cloudkitchen.rbac.service.UserService;
import com.cloudkitchen.rbac.util.HttpResponseUtil;
import com.cloudkitchen.rbac.util.ResponseBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "User read operations - Super Admin only")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Get All Users", description = "Get all users (customers, merchants, admins) - Super Admin only")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        try {
            List<Object> response = userService.getAllUsers();
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ResponseBuilder.success(HttpResponseUtil.OK, 
                          "All users retrieved successfully. Total: " + response.size(), response));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(HttpResponseUtil.INTERNAL_SERVER_ERROR, 
                          "Internal server error while retrieving users"));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get User by ID", description = "Get any user by ID - Super Admin only")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable Integer id) {
        try {
            Object response = userService.getUserById(id);
            return ResponseEntity.status(HttpStatus.OK)
                    .body(ResponseBuilder.success(HttpResponseUtil.OK, 
                          "User retrieved successfully", response));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ResponseBuilder.error(HttpResponseUtil.NOT_FOUND, 
                              "User not found with ID: " + id));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ResponseBuilder.error(HttpResponseUtil.INTERNAL_SERVER_ERROR, 
                          "Internal server error while retrieving user"));
        }
    }
}
