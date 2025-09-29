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
    private final MerchantRepository merchantRepository;

    public DataInitializer(RoleRepository roleRepository, 
                          PermissionRepository permissionRepository,
                          RolePermissionRepository rolePermissionRepository,
                          MerchantRepository merchantRepository) {
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.merchantRepository = merchantRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        initializeMerchants();
        initializeRoles();
        initializePermissions();
        assignPermissions();
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
                permissionRepository.findAll().forEach(permission -> {
                    RolePermission rp = new RolePermission();
                    rp.setRole(superAdmin);
                    rp.setPermission(permission);
                    rp.setCreatedBy(1);
                    rolePermissionRepository.save(rp);
                });
            }
            
            // Assign merchant permissions to merchant_admin (exclude merchant creation/deletion)
            if (merchantAdmin != null) {
                permissionRepository.findAll().forEach(permission -> {
                    if (!permission.getPermissionName().equals("merchants.create") && 
                        !permission.getPermissionName().equals("merchants.delete")) {
                        RolePermission rp = new RolePermission();
                        rp.setRole(merchantAdmin);
                        rp.setPermission(permission);
                        rp.setCreatedBy(1);
                        rolePermissionRepository.save(rp);
                    }
                });
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
                        rp.setCreatedBy(1);
                        rolePermissionRepository.save(rp);
                    }
                }
            }
        }
    }

    private void initializeMerchants() {
        if (merchantRepository.count() == 0) {
            merchantRepository.save(createMerchant("Demo Restaurant", "demo@restaurant.com", "9876543210", "123 Main St"));
            merchantRepository.save(createMerchant("Pizza Palace", "info@pizzapalace.com", "9876543211", "456 Oak Ave"));
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