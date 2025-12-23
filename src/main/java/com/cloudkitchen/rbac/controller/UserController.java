package com.cloudkitchen.rbac.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cloudkitchen.rbac.dto.common.PageRequest;
import com.cloudkitchen.rbac.dto.common.PageResponse;
import com.cloudkitchen.rbac.dto.user.UserResponse;
import com.cloudkitchen.rbac.service.UserService;
import com.cloudkitchen.rbac.util.HttpResponseUtil;
import com.cloudkitchen.rbac.util.ResponseBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "User read operations - Super Admin only")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Get All Users", description = "Get all users with pagination, filtering by role, and search - Super Admin only")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestParam(defaultValue = "0") String page,
            @RequestParam(defaultValue = "20") String size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search) {

        // Validate page parameter
        int pageNum;
        try {
            pageNum = Integer.parseInt(page);
            if (pageNum < 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseBuilder.error(HttpResponseUtil.BAD_REQUEST,
                                "Invalid page parameter. Must be a non-negative integer"));
            }
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseBuilder.error(HttpResponseUtil.BAD_REQUEST,
                            "Invalid page parameter. Must be a valid integer"));
        }

        // Validate size parameter
        int sizeNum;
        try {
            sizeNum = Integer.parseInt(size);
            if (sizeNum < 1 || sizeNum > 100) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ResponseBuilder.error(HttpResponseUtil.BAD_REQUEST,
                                "Invalid size parameter. Must be between 1 and 100"));
            }
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseBuilder.error(HttpResponseUtil.BAD_REQUEST,
                            "Invalid size parameter. Must be a valid integer"));
        }

        PageRequest pageRequest = new PageRequest(pageNum, sizeNum, sortBy, sortDirection);

        PageResponse<UserResponse> response = userService.getAllUsers(pageRequest, role, search);

        String message = String.format(
            "Users retrieved successfully. Page %d of %d, Total: %d",
            response.getPage() + 1,
            response.getTotalPages(),
            response.getTotalElements()
        );

        if (role != null && !role.trim().isEmpty()) {
            message += " (filtered by role: " + role + ")";
        }
        if (search != null && !search.trim().isEmpty()) {
            message += " (search: " + search + ")";
        }

        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseBuilder.success(HttpResponseUtil.OK, message, response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get User by ID", description = "Get any user by ID - Super Admin only")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable Integer id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ResponseBuilder.success(HttpResponseUtil.OK,
                        "User retrieved successfully", response));
    }
}
