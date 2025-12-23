package com.cloudkitchen.rbac.service.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cloudkitchen.rbac.domain.entity.User;
import com.cloudkitchen.rbac.dto.common.PageRequest;
import com.cloudkitchen.rbac.dto.common.PageResponse;
import com.cloudkitchen.rbac.dto.user.UserResponse;
import com.cloudkitchen.rbac.repository.UserRepository;
import com.cloudkitchen.rbac.service.UserService;
import com.cloudkitchen.rbac.exception.BusinessExceptions.*;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    // Valid sortable fields for User entity
    private static final Set<String> VALID_SORT_FIELDS = new HashSet<>(Arrays.asList(
        "userId", "firstName", "lastName", "email", "phone", "username",
        "userType", "active", "verified", "createdOn", "lastLoginAt"
    ));

    // Valid user roles
    private static final Set<String> VALID_USER_ROLES = new HashSet<>(Arrays.asList(
        "customer", "merchant", "admin", "super_admin"
    ));

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public PageResponse<UserResponse> getAllUsers(PageRequest pageRequest, String role, String search) {
        // Validate role parameter
        if (role != null && !role.trim().isEmpty() && !VALID_USER_ROLES.contains(role.trim().toLowerCase())) {
            throw new IllegalArgumentException("Invalid role. Valid roles are: customer, merchant, admin, super_admin");
        }

        // Validate sortBy field
        if (pageRequest.getSortBy() != null && !pageRequest.getSortBy().trim().isEmpty()) {
            if (!VALID_SORT_FIELDS.contains(pageRequest.getSortBy().trim())) {
                throw new IllegalArgumentException("Invalid sortBy field. Valid fields are: " + String.join(", ", VALID_SORT_FIELDS));
            }
        }

        Pageable pageable = createPageable(pageRequest);

        String roleFilter = (role != null && !role.trim().isEmpty()) ? role.trim().toLowerCase() : null;
        String searchFilter = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        Page<User> userPage = userRepository.findAllUsersWithFilters(roleFilter, searchFilter, pageable);

        List<UserResponse> content = userPage.getContent().stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(
                content,
                userPage.getNumber(),
                userPage.getSize(),
                userPage.getTotalElements()
        );
    }

    @Override
    public UserResponse getUserById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id));
        return mapToUserResponse(user);
    }

    private Pageable createPageable(PageRequest pageRequest) {
        Sort sort = Sort.unsorted();

        if (pageRequest.getSortBy() != null && !pageRequest.getSortBy().trim().isEmpty()) {
            Sort.Direction direction = "desc".equalsIgnoreCase(pageRequest.getSortDirection())
                    ? Sort.Direction.DESC
                    : Sort.Direction.ASC;
            sort = Sort.by(direction, pageRequest.getSortBy().trim());
        } else {
            // Default deterministic sorting by userId ascending
            sort = Sort.by(Sort.Direction.ASC, "userId");
        }

        return org.springframework.data.domain.PageRequest.of(pageRequest.getPage(), pageRequest.getSize(), sort);
    }

    private UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getUserId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setPhone(user.getPhone());
        response.setEmail(user.getEmail());
        response.setUsername(user.getUsername());
        response.setUserType(user.getUserType());
        response.setMerchantId(user.getMerchant() != null ? user.getMerchant().getMerchantId() : null);
        response.setMerchantName(user.getMerchant() != null ? user.getMerchant().getMerchantName() : null);
        response.setActive(user.getActive());
        response.setVerified(user.getVerified());
        response.setCreatedAt(user.getCreatedOn());
        response.setLastLogin(user.getLastLoginAt());
        return response;
    }
}
