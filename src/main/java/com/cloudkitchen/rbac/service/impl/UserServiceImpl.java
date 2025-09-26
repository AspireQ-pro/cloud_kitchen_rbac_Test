package com.cloudkitchen.rbac.service.impl;

import com.cloudkitchen.rbac.domain.entity.*;
import com.cloudkitchen.rbac.dto.auth.RegisterRequest;
import com.cloudkitchen.rbac.dto.customer.CustomerResponse;
import com.cloudkitchen.rbac.exception.AuthExceptions;
import com.cloudkitchen.rbac.repository.*;
import com.cloudkitchen.rbac.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final MerchantRepository merchantRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder encoder;

    public UserServiceImpl(UserRepository userRepository, MerchantRepository merchantRepository,
                          RoleRepository roleRepository, UserRoleRepository userRoleRepository,
                          PasswordEncoder encoder) {
        this.userRepository = userRepository;
        this.merchantRepository = merchantRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.encoder = encoder;
    }

    @Override
    public List<CustomerResponse> getAllUsers(Integer requestingUserId) {
        if (requestingUserId == null) {
            throw new IllegalArgumentException("Requesting user ID cannot be null");
        }
        
        User requestingUser = getUser(requestingUserId);
        
        // Only super_admin can see all users
        if (!"super_admin".equals(requestingUser.getUserType())) {
            throw new AuthExceptions.AccessDeniedException("Access denied");
        }
        
        try {
            return userRepository.findAll().stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve users", e);
        }
    }

    @Override
    public CustomerResponse getUserById(Integer userId, Integer requestingUserId) {
        if (userId == null || requestingUserId == null) {
            throw new IllegalArgumentException("User ID and requesting user ID cannot be null");
        }
        
        User requestingUser = getUser(requestingUserId);
        User user = getUser(userId);
        
        // Super admin can see any user
        if ("super_admin".equals(requestingUser.getUserType())) {
            return mapToResponse(user);
        }
        
        // Merchant can see users in their merchant
        if ("merchant".equals(requestingUser.getUserType())) {
            Integer requestingMerchantId = requestingUser.getMerchant() != null ? requestingUser.getMerchant().getMerchantId() : null;
            Integer userMerchantId = user.getMerchant() != null ? user.getMerchant().getMerchantId() : null;
            if (!Objects.equals(requestingMerchantId, userMerchantId)) {
                throw new AuthExceptions.AccessDeniedException("Access denied");
            }
            return mapToResponse(user);
        }
        
        // Users can see their own profile
        if (userId.equals(requestingUserId)) {
            return mapToResponse(user);
        }
        
        throw new AuthExceptions.AccessDeniedException("Access denied");
    }

    @Override
    @Transactional
    public CustomerResponse createUser(RegisterRequest req, Integer requestingUserId) {
        if (req == null || requestingUserId == null) {
            throw new IllegalArgumentException("Request and requesting user ID cannot be null");
        }
        
        if (req.getPhone() == null || req.getPhone().trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number is required");
        }
        
        if (req.getFirstName() == null || req.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required");
        }
        
        if (req.getPassword() == null || req.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
        
        User requestingUser = getUser(requestingUserId);
        
        // Only super_admin can create users
        if (!"super_admin".equals(requestingUser.getUserType())) {
            throw new AuthExceptions.AccessDeniedException("Access denied");
        }
        
        // Validate merchant exists if provided
        Merchant merchant = null;
        if (req.getMerchantId() != null) {
            merchant = merchantRepository.findById(req.getMerchantId())
                    .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));
        }
        
        // Check for duplicate phone
        if (req.getMerchantId() != null) {
            if (userRepository.findByPhoneAndMerchant_MerchantId(req.getPhone(), req.getMerchantId()).isPresent()) {
                throw new AuthExceptions.UserAlreadyExistsException("Phone already registered");
            }
        } else {
            if (userRepository.findByPhoneAndMerchantIsNull(req.getPhone()).isPresent()) {
                throw new AuthExceptions.UserAlreadyExistsException("Phone already registered");
            }
        }
        
        try {
            User user = new User();
            user.setMerchant(merchant);
            user.setPhone(req.getPhone());
            user.setUsername(req.getPhone());
            user.setFirstName(req.getFirstName());
            user.setLastName(req.getLastName());
            user.setUserType(req.getUserType());
            user.setPasswordHash(encoder.encode(req.getPassword()));
            user.setAddress(req.getAddress());
            user.setCreatedBy(requestingUserId);
            
            userRepository.save(user);
            
            // Assign role
            assignRole(user, req.getUserType(), merchant);
            
            return mapToResponse(user);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create user", e);
        }
    }

    @Override
    @Transactional
    public CustomerResponse updateUser(Integer userId, RegisterRequest req, Integer requestingUserId) {
        if (userId == null || req == null || requestingUserId == null) {
            throw new IllegalArgumentException("User ID, request, and requesting user ID cannot be null");
        }
        
        User requestingUser = getUser(requestingUserId);
        User user = getUser(userId);
        
        try {
            // Super admin can update any user
            if ("super_admin".equals(requestingUser.getUserType())) {
                updateUserFields(user, req, requestingUserId);
                userRepository.save(user);
                return mapToResponse(user);
            }
            
            // Users can update their own profile (limited fields)
            if (userId.equals(requestingUserId)) {
                user.setFirstName(req.getFirstName());
                user.setLastName(req.getLastName());
                user.setAddress(req.getAddress());
                user.setUpdatedBy(requestingUserId);
                userRepository.save(user);
                return mapToResponse(user);
            }
            
            throw new AuthExceptions.AccessDeniedException("Access denied");
        } catch (AuthExceptions.AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to update user", e);
        }
    }

    @Override
    @Transactional
    public void deleteUser(Integer userId, Integer requestingUserId) {
        if (userId == null || requestingUserId == null) {
            throw new IllegalArgumentException("User ID and requesting user ID cannot be null");
        }
        
        User requestingUser = getUser(requestingUserId);
        
        // Only super_admin can delete users
        if (!"super_admin".equals(requestingUser.getUserType())) {
            throw new AuthExceptions.AccessDeniedException("Access denied");
        }
        
        // Prevent self-deletion
        if (userId.equals(requestingUserId)) {
            throw new IllegalArgumentException("Cannot delete your own account");
        }
        
        try {
            User user = getUser(userId);
            userRepository.delete(user);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete user", e);
        }
    }

    private User getUser(Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new AuthExceptions.UserNotFoundException("User not found with ID: " + userId));
    }

    private void updateUserFields(User user, RegisterRequest req, Integer updatedBy) {
        if (user == null || req == null) {
            throw new IllegalArgumentException("User and request cannot be null");
        }
        
        try {
            if (req.getFirstName() != null) user.setFirstName(req.getFirstName());
            if (req.getLastName() != null) user.setLastName(req.getLastName());
            if (req.getAddress() != null) user.setAddress(req.getAddress());
            if (req.getPassword() != null) user.setPasswordHash(encoder.encode(req.getPassword()));
            user.setUpdatedBy(updatedBy);
        } catch (Exception e) {
            throw new RuntimeException("Failed to update user fields", e);
        }
    }

    private void assignRole(User user, String userType, Merchant merchant) {
        if (user == null || userType == null) {
            throw new IllegalArgumentException("User and user type cannot be null");
        }
        
        try {
            String roleName = switch (userType) {
                case "super_admin" -> "super_admin";
                case "merchant" -> "merchant_admin";
                case "customer" -> "customer";
                default -> "customer";
            };
            
            Role role = roleRepository.findByRoleName(roleName)
                    .orElseThrow(() -> new IllegalStateException("Role not found: " + roleName));
            
            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(role);
            userRole.setMerchant(merchant);
            userRole.setAssignedAt(LocalDateTime.now());
            userRoleRepository.save(userRole);
        } catch (Exception e) {
            throw new RuntimeException("Failed to assign role to user", e);
        }
    }

    private CustomerResponse mapToResponse(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }
        
        try {
            CustomerResponse response = new CustomerResponse();
            response.setUserId(user.getUserId());
            response.setFirstName(user.getFirstName());
            response.setLastName(user.getLastName());
            response.setPhone(user.getPhone());
            response.setEmail(user.getEmail());
            response.setAddress(user.getAddress());
            if (user.getMerchant() != null) {
                response.setMerchantId(user.getMerchant().getMerchantId());
                response.setMerchantName(user.getMerchant().getMerchantName());
            }
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to map user to response", e);
        }
    }
}