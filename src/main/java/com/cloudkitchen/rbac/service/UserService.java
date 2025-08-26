package com.cloudkitchen.rbac.service;

import com.cloudkitchen.rbac.domain.entity.User;
import java.util.List;

public interface UserService {
    List<User> getAllUsers();
    User getUserById(Integer id);
    User updateUser(Integer id, User user);
    void deleteUser(Integer id);
}