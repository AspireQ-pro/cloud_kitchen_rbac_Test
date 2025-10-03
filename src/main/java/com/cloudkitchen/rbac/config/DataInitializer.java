package com.cloudkitchen.rbac.config;
import org.springframework.boot.CommandLineRunner;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import org.springframework.security.crypto.password.PasswordEncoder;

@Component
public class DataInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final MerchantRepository merchantRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppConstants appConstants;

    public DataInitializer(RoleRepository roleRepository, 
                          PermissionRepository permissionRepository,
                          RolePermissionRepository rolePermissionRepository,
                          MerchantRepository merchantRepository,
                          UserRepository userRepository,
                          UserRoleRepository userRoleRepository,
                          PasswordEncoder passwordEncoder,
                          AppConstants appConstants) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.merchantRepository = merchantRepository;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.appConstants = appConstants;
    }

    @Override
    @Transactional
    public void run(String... args) {
        try {
            initializeMerchants();
            initializeRoles();
            initializePermissions();
            assignPermissions();
            initializeUsers();
            log.info("Database initialization completed successfully");
        } catch (Exception e) {
            log.error("Database initialization failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize database", e);
        }
    }



    private void initializeRoles() {
        if (roleRepository.count() == 0) {
            // Create roles that match schema exactly
            roleRepository.save(createRole("super_admin", "Super Administrator with full system access", true));
            roleRepository.save(createRole("merchant_admin", "Merchant Administrator with full merchant access", true));
            roleRepository.save(createRole("merchant_manager", "Merchant Manager with limited admin access", true));
            roleRepository.save(createRole("merchant_staff", "Merchant Staff with operational access", true));
            roleRepository.save(createRole("customer", "Customer with order and profile access", true));
            
            // Note: Users can only have user_type of 'super_admin', 'merchant', 'customer'
            // But roles table can have more granular roles for permission mapping
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
            
            // Order management (for future integration)
            permissionRepository.save(createPermission("orders.create", "orders", "create", "Create new orders"));
            permissionRepository.save(createPermission("orders.read", "orders", "read", "View orders"));
            permissionRepository.save(createPermission("orders.update", "orders", "update", "Update order status"));
            permissionRepository.save(createPermission("orders.delete", "orders", "delete", "Cancel/delete orders"));
            
            // Product management (for future integration)
            permissionRepository.save(createPermission("products.create", "products", "create", "Create new products"));
            permissionRepository.save(createPermission("products.read", "products", "read", "View products"));
            permissionRepository.save(createPermission("products.update", "products", "update", "Update product information"));
            permissionRepository.save(createPermission("products.delete", "products", "delete", "Delete products"));
            
            // Payment management (for future integration)
            permissionRepository.save(createPermission("payments.read", "payments", "read", "View payment information"));
            permissionRepository.save(createPermission("payments.process", "payments", "process", "Process payments"));
            permissionRepository.save(createPermission("payments.refund", "payments", "refund", "Process refunds"));
        }
    }

    private void assignPermissions() {
        if (rolePermissionRepository.count() == 0) {
            Role superAdmin = roleRepository.findByRoleName("super_admin").orElse(null);
            Role merchantAdmin = roleRepository.findByRoleName("merchant_admin").orElse(null);
            Role customer = roleRepository.findByRoleName("customer").orElse(null);
            
            // Assign all permissions to super_admin
            if (superAdmin != null) {
                for (Permission permission : permissionRepository.findAll()) {
                    RolePermission rp = new RolePermission();
                    rp.setRole(superAdmin);
                    rp.setPermission(permission);
                    rp.setCreatedBy(0); // System initialization
                    rolePermissionRepository.save(rp);
                }
            }
            
            // Assign merchant permissions to merchant_admin (exclude merchant creation/deletion)
            if (merchantAdmin != null) {
                for (Permission permission : permissionRepository.findAll()) {
                    if (!permission.getPermissionName().equals("merchants.create") && 
                        !permission.getPermissionName().equals("merchants.delete")) {
                        RolePermission rp = new RolePermission();
                        rp.setRole(merchantAdmin);
                        rp.setPermission(permission);
                        rp.setCreatedBy(0); // System initialization
                        rolePermissionRepository.save(rp);
                    }
                }
            }
            
            // Assign limited permissions to customer
            if (customer != null) {
                String[] customerPermissions = {"orders.create", "orders.read", "products.read", "users.update"};
                for (String permName : customerPermissions) {
                    Permission perm = permissionRepository.findByPermissionName(permName).orElse(null);
                    if (perm != null) {
                        RolePermission rp = new RolePermission();
                        rp.setRole(customer);
                        rp.setPermission(perm);
                        rp.setCreatedBy(0); // System initialization
                        rolePermissionRepository.save(rp);
                    }
                }
            }
        }
    }

    private void initializeMerchants() {
        // Create test merchant for API testing
        if (merchantRepository.count() == 0) {
            Merchant testMerchant = createMerchant(
                appConstants.getDefaultMerchantName(), 
                appConstants.getDefaultMerchantEmail(), 
                appConstants.getDefaultMerchantPhone(), 
                appConstants.getDefaultMerchantAddress()
            );
            merchantRepository.save(testMerchant);
            log.info("Created test merchant: {} (ID: {})", testMerchant.getMerchantName(), testMerchant.getMerchantId());
        }
    }
    
    private Merchant createMerchant(String name, String email, String phone, String address) {
        Merchant merchant = new Merchant();
        merchant.setMerchantName(name);
        merchant.setBusinessName(name); // Required field
        merchant.setEmail(email);
        merchant.setPhone(phone);
        merchant.setAddress(address);
        merchant.setActive(true);
        return merchant;
    }
    
    private Role createRole(String name, String description, boolean isSystem) {
        Role role = new Role();
        role.setRoleName(name);
        role.setDescription(description);
        role.setSystemRole(isSystem);
        role.setCreatedBy(0); // System initialization
        return role;
    }

    private Permission createPermission(String name, String resource, String action, String description) {
        Permission permission = new Permission();
        permission.setPermissionName(name);
        permission.setResource(resource);
        permission.setAction(action);
        permission.setDescription(description);
        permission.setCreatedBy(0); // System initialization
        return permission;
    }
    
    private User createUser(String phone, String firstName, String lastName, String email, String password, String userType, Merchant merchant, String username) {
        User user = new User();
        user.setPhone(phone);
        user.setUsername(username != null ? username : phone);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setUserType(userType);
        user.setMerchant(merchant);
        user.setActive(true);
        user.setEmailVerified(true);
        user.setPhoneVerified(true);
        user.setCreatedBy(0);
        return user;
    }

    private void initializeUsers() {
        // Create super admin user for testing
        if (userRepository.count() == 0) {
            // Super admin has no merchant (merchant = null)
            User superAdmin = createUser(
                "9999999999", 
                "Super", 
                "Admin", 
                "superadmin@cloudkitchen.com", 
                "Admin123!", 
                "super_admin", 
                null,  // Super admin has no merchant
                "superadmin"  // Username for login
            );
            userRepository.save(superAdmin);
            
            // Assign super_admin role
            Role superAdminRole = roleRepository.findByRoleName("super_admin").orElse(null);
            if (superAdminRole != null) {
                UserRole userRole = new UserRole();
                userRole.setUser(superAdmin);
                userRole.setRole(superAdminRole);
                userRole.setMerchant(null); // Super admin role has no merchant
                userRole.setCreatedBy(0);
                userRoleRepository.save(userRole);
            }
            
            log.info("Created super admin user: {} (ID: {})", superAdmin.getPhone(), superAdmin.getUserId());
            
            // Create merchant admin user for testing
            Merchant testMerchant = merchantRepository.findAll().stream().findFirst().orElse(null);
            if (testMerchant != null) {
                User merchantAdmin = createUser(
                    "9876543210", 
                    "Merchant", 
                    "Admin", 
                    "merchant@testmerchant.com", 
                    "Merchant123!", 
                    "merchant", 
                    null,  // Merchant admin has no specific merchant in user table
                    "merchantadmin"  // Username for login
                );
                userRepository.save(merchantAdmin);
                
                // Assign merchant_admin role
                Role merchantAdminRole = roleRepository.findByRoleName("merchant_admin").orElse(null);
                if (merchantAdminRole != null) {
                    UserRole userRole = new UserRole();
                    userRole.setUser(merchantAdmin);
                    userRole.setRole(merchantAdminRole);
                    userRole.setMerchant(testMerchant); // Associate with test merchant
                    userRole.setCreatedBy(0);
                    userRoleRepository.save(userRole);
                }
                
                log.info("Created merchant admin user: {} (ID: {})", merchantAdmin.getPhone(), merchantAdmin.getUserId());
            }
        }
    }
}