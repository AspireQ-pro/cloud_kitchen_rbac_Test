package com.cloudkitchen.rbac.service;

import com.cloudkitchen.rbac.dto.common.PageRequest;
import com.cloudkitchen.rbac.dto.common.PageResponse;
import com.cloudkitchen.rbac.dto.user.UserResponse;

public interface UserService {
    PageResponse<UserResponse> getAllUsers(PageRequest pageRequest, String role, String search);
    UserResponse getUserById(Integer id);
}
