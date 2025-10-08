package com.cloudkitchen.rbac.config;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.cloudkitchen.rbac.domain.entity.Merchant;
import com.cloudkitchen.rbac.domain.entity.Permission;
import com.cloudkitchen.rbac.domain.entity.Role;
import com.cloudkitchen.rbac.domain.entity.RolePermission;
import com.cloudkitchen.rbac.domain.entity.User;
import com.cloudkitchen.rbac.domain.entity.UserRole;
import com.cloudkitchen.rbac.repository.MerchantRepository;
import com.cloudkitchen.rbac.repository.PermissionRepository;
import com.cloudkitchen.rbac.repository.RolePermissionRepository;
import com.cloudkitchen.rbac.repository.RoleRepository;
import com.cloudkitchen.rbac.repository.UserRepository;
import com.cloudkitchen.rbac.repository.UserRoleRepository;

@Component
public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    
    @org.springframework.beans.factory.annotation.Value("${app.data.initialize:true}")
    private boolean initializeData;
    
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final MerchantRepository merchantRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(RoleRepository roleRepository, 
                          PermissionRepository permissionRepository,
                          RolePermissionRepository rolePermissionRepository,
                          MerchantRepository merchantRepository,
                          UserRepository userRepository,
                          UserRoleRepository userRoleRepository,
                          PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.merchantRepository = merchantRepository;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!initializeData) {
            log.info("Data initialization disabled (app.data.initialize=false)");
            return;
        }
        
        try {
            initializeRoles();
            initializePermissions();
            assignPermissions();
            initializeMerchants();
            initializeAdminUser();
            log.info("Database initialization completed successfully");
        } catch (Exception e) {
            log.error("Database initialization failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    private void initializeRoles() {
        String[] roleNames = {"super_admin", "merchant", "merchant_manager", "merchant_staff", "customer"};
        
        long existingRoles = roleRepository.count();
        if (existingRoles == 0) {
            for (String roleName : roleNames) {
                Role role = new Role();
                role.setRoleName(roleName);
                role.setDescription(getRoleDescription(roleName));
                role.setCreatedOn(LocalDateTime.now());
                roleRepository.save(role);
                log.info("Created role: {}", roleName);
            }
        } else {
            log.info("Roles already exist, skipping initialization. Found {} roles", existingRoles);
        }
    }

    private void initializePermissions() {
        String[] permissions = {
            "user:read", "user:write", "user:delete",
            "merchant:read", "merchant:write", "merchant:delete",
            "customer:read", "customer:write", "customer:delete",
            "role:read", "role:write", "role:delete",
            "permission:read", "permission:write", "permission:delete",
            "order:read", "order:write", "order:delete",
            "menu:read", "menu:write", "menu:delete",
            "profile:read", "profile:write"
        };
        
        long existingPermissions = permissionRepository.count();
        if (existingPermissions == 0) {
            for (String permName : permissions) {
                Permission permission = new Permission();
                permission.setPermissionName(permName);
                permission.setDescription(getPermissionDescription(permName));
                String[] parts = permName.split(":");
                permission.setResource(parts.length > 0 ? parts[0] : "unknown");
                permission.setAction(parts.length > 1 ? parts[1] : "unknown");
                permission.setCreatedOn(LocalDateTime.now());
                permissionRepository.save(permission);
            }
            log.info("Created {} permissions", permissions.length);
        } else {
            log.info("Permissions already exist, skipping initialization. Found {} permissions", existingPermissions);
        }
    }

    private void assignPermissions() {
        assignRolePermissions("super_admin", Arrays.asList(
            "user:read", "user:write", "user:delete",
            "merchant:read", "merchant:write", "merchant:delete",
            "customer:read", "customer:write", "customer:delete",
            "role:read", "role:write", "role:delete",
            "permission:read", "permission:write", "permission:delete",
            "order:read", "order:write", "order:delete",
            "menu:read", "menu:write", "menu:delete",
            "profile:read", "profile:write"
        ));
        
        assignRolePermissions("merchant", Arrays.asList(
            "user:read", "user:write", "customer:read", "customer:write",
            "order:read", "order:write", "menu:read", "menu:write", "profile:read", "profile:write"
        ));
        
        assignRolePermissions("merchant_manager", Arrays.asList(
            "customer:read", "order:read", "order:write", "menu:read", "profile:read", "profile:write"
        ));
        
        assignRolePermissions("merchant_staff", Arrays.asList(
            "order:read", "menu:read", "profile:read"
        ));
        
        assignRolePermissions("customer", Arrays.asList(
            "order:read", "order:write", "menu:read", "profile:read", "profile:write"
        ));
    }

    private void initializeMerchants() {
        if (merchantRepository.count() == 0) {
            Merchant defaultMerchant = new Merchant();
            defaultMerchant.setMerchantName("Default Restaurant");
            defaultMerchant.setEmail("admin@defaultrestaurant.com");
            defaultMerchant.setPhone("1234567890");
            defaultMerchant.setBusinessName("Default Restaurant Business");
            defaultMerchant.setAddress("123 Main Street, City");
            defaultMerchant.setActive(true);
            defaultMerchant.setCreatedOn(LocalDateTime.now());
            merchantRepository.save(defaultMerchant);
            log.info("Created default merchant with ID: {}", defaultMerchant.getMerchantId());
        }
    }

    private void initializeAdminUser() {
        if (userRepository.findByUsernameAndMerchantIsNull("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPhone("9999999999");
            admin.setEmail("admin@cloudkitchen.com");
            admin.setFirstName("Super");
            admin.setLastName("Admin");
            admin.setUserType("super_admin");
            admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
            admin.setActive(true);
            admin.setCreatedOn(LocalDateTime.now());
            admin = userRepository.save(admin);
            
            Role superAdminRole = roleRepository.findByRoleName("super_admin")
                .orElseThrow(() -> new RuntimeException("Super admin role not found"));
            
            UserRole userRole = new UserRole();
            userRole.setUser(admin);
            userRole.setRole(superAdminRole);
            userRole.setAssignedAt(LocalDateTime.now());
            userRoleRepository.save(userRole);
            
            log.info("Created super admin user: admin/Admin@123");
        }
    }

    private void assignRolePermissions(String roleName, List<String> permissionNames) {
        Role role = roleRepository.findByRoleName(roleName).orElse(null);
        if (role == null) {
            log.warn("Role not found: {}", roleName);
            return;
        }
        
        int assignedCount = 0;
        for (String permName : permissionNames) {
            Permission permission = permissionRepository.findByPermissionName(permName).orElse(null);
            if (permission != null && !rolePermissionRepository.existsByRoleAndPermission(role, permission)) {
                RolePermission rp = new RolePermission();
                rp.setRole(role);
                rp.setPermission(permission);
                rp.setCreatedOn(LocalDateTime.now());
                rolePermissionRepository.save(rp);
                assignedCount++;
            }
        }
        if (assignedCount > 0) {
            log.info("Assigned {} permissions to role: {}", assignedCount, roleName);
        }
    }

    private String getRoleDescription(String roleName) {
        return switch (roleName) {
            case "super_admin" -> "Super administrator with full system access";
            case "merchant" -> "Merchant administrator with full merchant access";
            case "merchant_manager" -> "Merchant manager with limited administrative access";
            case "merchant_staff" -> "Merchant staff with basic operational access";
            case "customer" -> "Customer with order and profile access";
            default -> "Role description not available";
        };
    }

    private String getPermissionDescription(String permissionName) {
        if (permissionName == null || !permissionName.contains(":")) {
            return "Permission description not available";
        }
        
        String[] parts = permissionName.split(":");
        String resource = parts[0];
        String action = parts[1];
        
        String actionDesc = switch (action) {
            case "read" -> "Read";
            case "write" -> "Create and update";
            case "delete" -> "Delete";
            default -> "Manage";
        };
        
        String resourceDesc = switch (resource) {
            case "user" -> "user information";
            case "merchant" -> "merchant information";
            case "customer" -> "customer information";
            case "role" -> "role information";
            case "permission" -> "permission information";
            case "order" -> "order information";
            case "menu" -> "menu items";
            case "profile" -> "profile information";
            default -> resource + " information";
        };
        
        return actionDesc + " " + resourceDesc;
    }
}