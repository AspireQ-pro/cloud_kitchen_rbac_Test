package com.cloudkitchen.rbac.service;

import com.cloudkitchen.rbac.domain.entity.User;
import java.util.List;

public interface SecurityService {
    boolean hasPermission(Integer userId, String permission);
    boolean hasRole(Integer userId, String role);
    List<String> getUserPermissions(Integer userId, Integer merchantId);
    List<String> getUserRoles(Integer userId, Integer merchantId);
    boolean canAccessResource(Integer userId, String resource, String action);
    void validateUserAccess(Integer requestingUserId, Integer targetUserId);
    User getCurrentUser();
}