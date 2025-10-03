package com.cloudkitchen.rbac.config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.cloudkitchen.rbac.repository.PermissionRepository;
import com.cloudkitchen.rbac.repository.RolePermissionRepository;
import com.cloudkitchen.rbac.repository.RoleRepository;

@Component
public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    
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
        try {
            initializeRoles();
            initializePermissions();
            assignPermissions();
            log.info("Database initialization completed successfully");
        } catch (Exception e) {
            log.error("Database initialization failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }



    private void initializeRoles() {
        // Roles are managed through database schema initialization
        // Check if roles exist, if not log warning
        long roleCount = roleRepository.count();
        if (roleCount == 0) {
            log.warn("No roles found in database. Please run the database schema initialization script.");
        } else {
            log.info("Found {} roles in database", roleCount);
        }
    }

    private void initializePermissions() {
        // Permissions are managed through database schema initialization
        long permissionCount = permissionRepository.count();
        if (permissionCount == 0) {
            log.warn("No permissions found in database. Please run the database schema initialization script.");
        } else {
            log.info("Found {} permissions in database", permissionCount);
        }
    }

    private void assignPermissions() {
        // Role permissions are managed through database schema initialization
        long rolePermissionCount = rolePermissionRepository.count();
        if (rolePermissionCount == 0) {
            log.warn("No role permissions found in database. Please run the database schema initialization script.");
        } else {
            log.info("Found {} role permission mappings in database", rolePermissionCount);
        }
    }


}