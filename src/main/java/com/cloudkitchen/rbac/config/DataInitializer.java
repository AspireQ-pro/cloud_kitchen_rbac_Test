package com.cloudkitchen.rbac.config;

import com.cloudkitchen.rbac.domain.entity.*;
import com.cloudkitchen.rbac.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {
    
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public DataInitializer(RoleRepository roleRepository, 
                          PermissionRepository permissionRepository,
                          RolePermissionRepository rolePermissionRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        initializeRoles();
        initializePermissions();
        assignPermissions();
    }

    private void initializeRoles() {
        if (roleRepository.count() == 0) {
            roleRepository.save(createRole("super_admin", "Super Administrator with full system access", true));
            roleRepository.save(createRole("merchant_admin", "Merchant Administrator with full merchant access", true));
            roleRepository.save(createRole("merchant_manager", "Merchant Manager with limited admin access", true));
            roleRepository.save(createRole("merchant_staff", "Merchant Staff with operational access", true));
            roleRepository.save(createRole("customer", "Customer with order and profile access", true));
        }
    }

    private void initializePermissions() {
        if (permissionRepository.count() == 0) {
            // User permissions
            permissionRepository.save(createPermission("users.create", "users", "create", "Create new users"));
            permissionRepository.save(createPermission("users.read", "users", "read", "View user details"));
            permissionRepository.save(createPermission("users.update", "users", "update", "Update user information"));
            permissionRepository.save(createPermission("users.delete", "users", "delete", "Delete users"));
            
            // Merchant permissions
            permissionRepository.save(createPermission("merchants.create", "merchants", "create", "Create new merchants"));
            permissionRepository.save(createPermission("merchants.read", "merchants", "read", "View merchant details"));
            permissionRepository.save(createPermission("merchants.update", "merchants", "update", "Update merchant information"));
            permissionRepository.save(createPermission("merchants.delete", "merchants", "delete", "Delete merchants"));
            
            // Role permissions
            permissionRepository.save(createPermission("roles.create", "roles", "create", "Create new roles"));
            permissionRepository.save(createPermission("roles.read", "roles", "read", "View roles"));
            permissionRepository.save(createPermission("roles.update", "roles", "update", "Update roles"));
            permissionRepository.save(createPermission("roles.delete", "roles", "delete", "Delete roles"));
            permissionRepository.save(createPermission("roles.assign", "roles", "assign", "Assign roles to users"));
        }
    }

    private void assignPermissions() {
        if (rolePermissionRepository.count() == 0) {
            Role superAdmin = roleRepository.findByRoleName("super_admin").orElse(null);
            Role customer = roleRepository.findByRoleName("customer").orElse(null);
            
            if (superAdmin != null) {
                permissionRepository.findAll().forEach(permission -> {
                    RolePermission rp = new RolePermission();
                    rp.setRole(superAdmin);
                    rp.setPermission(permission);
                    rp.setCreatedBy(1);
                    rolePermissionRepository.save(rp);
                });
            }
            
            if (customer != null) {
                Permission usersUpdate = permissionRepository.findByPermissionName("users.update").orElse(null);
                if (usersUpdate != null) {
                    RolePermission rp = new RolePermission();
                    rp.setRole(customer);
                    rp.setPermission(usersUpdate);
                    rp.setCreatedBy(1);
                    rolePermissionRepository.save(rp);
                }
            }
        }
    }

    private Role createRole(String name, String description, boolean isSystem) {
        Role role = new Role();
        role.setRoleName(name);
        role.setDescription(description);
        role.setSystemRole(isSystem);
        role.setCreatedBy(1);
        return role;
    }

    private Permission createPermission(String name, String resource, String action, String description) {
        Permission permission = new Permission();
        permission.setPermissionName(name);
        permission.setResource(resource);
        permission.setAction(action);
        permission.setDescription(description);
        permission.setCreatedBy(1);
        return permission;
    }
}