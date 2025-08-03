package com.cloudkitchen.rbac.service;

import com.cloudkitchen.rbac.domain.User;
import java.util.List;
import java.util.Optional;

public interface UserService {
    User saveUser(User user);
    Optional<User> findByUsername(String username);
    List<User> getAllUsers();
}
