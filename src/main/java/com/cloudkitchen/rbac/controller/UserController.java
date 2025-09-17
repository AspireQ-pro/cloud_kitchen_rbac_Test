package com.cloudkitchen.rbac.controller;

import com.cloudkitchen.rbac.domain.entity.User;
import com.cloudkitchen.rbac.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

        @GetMapping("/customers")
        public ResponseEntity<List<User>> getAllCustomers() {
            return ResponseEntity.ok(userService.getAllCustomers());
        }

        @GetMapping("/customers/merchant/{merchantId}")
        public ResponseEntity<List<User>> getCustomersByMerchantId(@PathVariable Integer merchantId) {
            return ResponseEntity.ok(userService.getCustomersByMerchantId(merchantId));
        }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Integer id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Integer id, @RequestBody User user) {
        return ResponseEntity.ok(userService.updateUser(id, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}