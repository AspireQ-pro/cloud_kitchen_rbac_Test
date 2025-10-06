package com.cloudkitchen.rbac.service;

import java.util.List;

public interface UserService {
    List<Object> getAllUsers();
    Object getUserById(Integer id);
}
