package com.cloudkitchen.rbac.service;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import com.cloudkitchen.rbac.dto.common.PageRequest;
import com.cloudkitchen.rbac.dto.common.PageResponse;
import com.cloudkitchen.rbac.dto.user.UserResponse;

/**
 * User service contract for user lookup and response building.
 */
public interface UserService {
    /**
     * List users with pagination and optional filters.
     */
    PageResponse<UserResponse> getAllUsers(PageRequest pageRequest, String role, String search);
    /**
     * Fetch a user by ID.
     */
    UserResponse getUserById(Integer id);
    /**
     * Build the HTTP response for listing users.
     */
    ResponseEntity<Map<String, Object>> getAllUsersResponse(String page, String size, String sortBy, String sortDirection, String role, String search, Authentication authentication);
    /**
     * Build the HTTP response for fetching a user by ID.
     */
    ResponseEntity<Map<String, Object>> getUserByIdResponse(Integer id, Authentication authentication);
}
