package com.cloudkitchen.rbac.repository;

import com.cloudkitchen.rbac.domain.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, Integer> {
    @org.springframework.cache.annotation.Cacheable(value = "userRoles", key = "#userId + '_' + #merchantId")
    @Query("SELECT ur.role.roleName FROM UserRole ur " +
           "WHERE ur.user.userId = :userId AND " +
           "((:merchantId IS NULL AND ur.merchant IS NULL) OR ur.merchant.merchantId = :merchantId)")
    List<String> findRoleNames(Integer userId, Integer merchantId);
    
    @org.springframework.cache.annotation.Cacheable(value = "userPermissions", key = "#userId + '_' + #merchantId")
    @Query("SELECT DISTINCT p.permissionName FROM UserRole ur " +
           "JOIN ur.role r JOIN RolePermission rp ON r.roleId = rp.role.roleId " +
           "JOIN rp.permission p " +
           "WHERE ur.user.userId = :userId AND " +
           "((:merchantId IS NULL AND ur.merchant IS NULL) OR ur.merchant.merchantId = :merchantId)")
    List<String> findPermissionNames(Integer userId, Integer merchantId);
    
    boolean existsByUserAndRoleAndMerchant(com.cloudkitchen.rbac.domain.entity.User user, 
                                          com.cloudkitchen.rbac.domain.entity.Role role, 
                                          com.cloudkitchen.rbac.domain.entity.Merchant merchant);
    
    @Modifying
    @Transactional
    void deleteByUser(com.cloudkitchen.rbac.domain.entity.User user);
    
    boolean existsByUser_UserIdAndMerchant_MerchantId(Integer userId, Integer merchantId);
}
