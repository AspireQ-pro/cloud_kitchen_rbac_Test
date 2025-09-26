package com.cloudkitchen.rbac.service.impl;

import com.cloudkitchen.rbac.constants.ErrorCodes;
import com.cloudkitchen.rbac.domain.entity.User;
import com.cloudkitchen.rbac.exception.AuthExceptions;
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
    public boolean hasPermission(Integer userId, String permission) {
        if (userId == null || permission == null) return false;
        List<String> permissions = getUserPermissions(userId, null);
        return permissions.contains(permission);
    }
    
    @Override
    public boolean hasRole(Integer userId, String role) {
        if (userId == null || role == null) return false;
        List<String> roles = getUserRoles(userId, null);
        return roles.contains(role);
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
        String permission = resource + "." + action;
        return hasPermission(userId, permission);
    }
    
    @Override
    public void validateUserAccess(Integer requestingUserId, Integer targetUserId) {
        if (requestingUserId == null || targetUserId == null) {
            throw new AuthExceptions.AccessDeniedException(ErrorCodes.ACCESS_DENIED + ": Invalid user IDs");
        }
        
        // Users can access their own data
        if (requestingUserId.equals(targetUserId)) {
            return;
        }
        
        // Check if requesting user has admin privileges
        if (hasRole(requestingUserId, "super_admin") || hasRole(requestingUserId, "merchant_admin")) {
            return;
        }
        
        throw new AuthExceptions.AccessDeniedException(ErrorCodes.ACCESS_DENIED + ": Insufficient permissions");
    }
    
    @Override
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AuthExceptions.UserNotFoundException(ErrorCodes.USER_NOT_FOUND + ": No authenticated user");
        }
        
        try {
            Integer userId = Integer.valueOf(auth.getName());
            return userRepository.findById(userId)
                    .orElseThrow(() -> new AuthExceptions.UserNotFoundException(ErrorCodes.USER_NOT_FOUND + ": User not found"));
        } catch (NumberFormatException e) {
            throw new AuthExceptions.UserNotFoundException(ErrorCodes.USER_NOT_FOUND + ": Invalid user ID format");
        }
    }
}