package com.cloudkitchen.rbac.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cloudkitchen.rbac.service.UserService;

/**
 * HTTP adapter for user endpoints. Delegates authorization, business logic,
 * and response formatting to {@code UserService}.
 *
 * Related files: {@code com.cloudkitchen.rbac.service.UserService},
 * {@code com.cloudkitchen.rbac.service.impl.UserServiceImpl},
 * {@code com.cloudkitchen.rbac.dto.user.UserResponse},
 * exception handlers in {@code com.cloudkitchen.rbac.exception},
 * {@code com.cloudkitchen.rbac.util.AccessControlUtil},
 * {@code com.cloudkitchen.rbac.util.ResponseBuilder},
 * {@code src/main/resources/openapi/users.yaml}.
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    /**
     * Construct the controller with the user service dependency.
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * List users with pagination, optional filters, and search.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestParam(defaultValue = "0") String page,
            @RequestParam(defaultValue = "20") String size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search,
            Authentication authentication) {
        return userService.getAllUsersResponse(page, size, sortBy, sortDirection, role, search, authentication);
    }

    /**
     * Fetch a user profile by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable Integer id, Authentication authentication) {
        return userService.getUserByIdResponse(id, authentication);
    }
}
