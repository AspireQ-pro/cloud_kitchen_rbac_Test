package com.cloudkitchen.rbac.util;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Centralized utility for role-based and ownership-based access control
 */
@Component
public class AccessControlUtil {
    
    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    private static final String ROLE_MERCHANT = "ROLE_MERCHANT";
    private static final String ROLE_CUSTOMER = "ROLE_CUSTOMER";
    
    /**
     * Check if authenticated user is Super Admin
     */
    public boolean isSuperAdmin(Authentication authentication) {
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> ROLE_SUPER_ADMIN.equals(auth.getAuthority()));
    }
    
    /**
     * Check if authenticated user is Merchant
     */
    public boolean isMerchant(Authentication authentication) {
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> ROLE_MERCHANT.equals(auth.getAuthority()));
    }
    
    /**
     * Check if authenticated user is Customer
     */
    public boolean isCustomer(Authentication authentication) {
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> ROLE_CUSTOMER.equals(auth.getAuthority()));
    }
    
    /**
     * Check if user has specific permission
     */
    public boolean hasPermission(Authentication authentication, String permission) {
        if (authentication == null || permission == null) return false;
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> permission.equals(auth.getAuthority()));
    }
    
    /**
     * Get user ID from authentication
     */
    public Integer getUserId(Authentication authentication) {
        if (authentication == null) return null;
        try {
            return Integer.valueOf(authentication.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
