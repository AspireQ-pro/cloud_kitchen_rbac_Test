package com.cloudkitchen.rbac.service;

import com.cloudkitchen.rbac.dto.auth.RegisterRequest;
import com.cloudkitchen.rbac.dto.customer.CustomerResponse;
import java.util.List;

public interface UserService {
    List<CustomerResponse> getAllUsers(Integer requestingUserId);
    CustomerResponse getUserById(Integer userId, Integer requestingUserId);
    CustomerResponse createUser(RegisterRequest req, Integer requestingUserId);
    CustomerResponse updateUser(Integer userId, RegisterRequest req, Integer requestingUserId);
    void deleteUser(Integer userId, Integer requestingUserId);
}