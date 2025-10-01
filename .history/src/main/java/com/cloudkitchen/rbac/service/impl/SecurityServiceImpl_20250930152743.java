package com.cloudkitchen.rbac.service.impl;


import com.cloudkitchen.rbac.domain.entity.User;

import com.cloudkitchen.rbac.repository.UserRepository;
import com.cloudkitchen.rbac.repository.UserRoleRepository;
import com.cloudkitchen.rbac.service.SecurityService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SecurityServiceImpl implements SecurityService {
    
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    
    public SecurityServiceImpl(UserRepository userRepository, UserRoleRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
    }
    
    @Override
    public boolean hasPermission(Integer userId, String permission, Integer merchantId) {
        if (userId == null || permission == null) return false;
        List<String> permissions = getUserPermissions(userId, merchantId);
        return permissions.contains(permission);
    }
    
    @Override
    public boolean hasRole(Integer userId, String role, Integer merchantId) {
        if (userId == null || role == null) return false;
        List<String> roles = getUserRoles(userId, merchantId);
        return roles.contains(role);
    }
    
    @Override
    public boolean canAccessMerchant(String username, Integer merchantId) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return false;
        
        // Super admin can access all merchants
        if (hasRole(user.getUserId(), "SUPER_ADMIN", null)) {
            return true;
        }
        
        // Users can only access their own merchant
        return user.getMerchant() != null && user.getMerchant().getMerchantId().equals(merchantId);
    }
    
    @Override
    public List<String> getUserPermissions(Integer userId, Integer merchantId) {
        return userRoleRepository.findPermissionNames(userId, merchantId);
    }
    
    @Override
    public List<String> getUserRoles(Integer userId, Integer merchantId) {
        return userRoleRepository.findRoleNames(userId, merchantId);
    }
    
    @Override
    public boolean canAccessResource(Integer userId, String resource, String action) {
        if (resource == null || action == null) {
            return false;
        }
        String permission = resource + "." + action;
        User user = userRepository.findById(userId).orElse(null);
        Integer merchantId = user != null && user.getMerchant() != null ? user.getMerchant().getMerchantId() : null;
        return hasPermission(userId, permission, merchantId);
    }
    
    @Override
    public void validateUserAccess(Integer requestingUserId, Integer targetUserId) {
        if (requestingUserId == null || targetUserId == null) {
            throw new RuntimeException("Invalid user IDs");
        }
        
        // Users can access their own data
        if (requestingUserId.equals(targetUserId)) {
            return;
        }
        
        // Check if requesting user has admin privileges
        User requestingUser = userRepository.findById(requestingUserId).orElse(null);
        Integer merchantId = requestingUser != null && requestingUser.getMerchant() != null ? requestingUser.getMerchant().getMerchantId() : null;
        
        if (hasRole(requestingUserId, "super_admin", null) || hasRole(requestingUserId, "merchant_admin", merchantId)) {
            return;
        }
        
        throw new RuntimeException("Insufficient permissions");
    }
    
    @Override
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new RuntimeException("No authenticated user");
        }
        
        try {
            Integer userId = Integer.valueOf(auth.getName());
            return userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid user ID format", e);
        }
    }
}