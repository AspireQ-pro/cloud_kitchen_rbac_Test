package com.cloudkitchen.rbac.repository;

import com.cloudkitchen.rbac.domain.entity.User;
import com.cloudkitchen.rbac.domain.entity.Merchant;
import com.cloudkitchen.rbac.dto.auth.LoginUserData;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    
    // OPTIMIZED LOGIN QUERY - Single query with all login data
    @Cacheable(value = "loginUserData", key = "#phone + '_' + #merchantId", unless = "#result == null")
    @Query(value = "SELECT u.user_id, u.phone, u.password_hash, u.user_type, " +
           "CASE WHEN u.merchant_id IS NOT NULL THEN u.merchant_id ELSE :merchantId END, " +
           "c.customer_id, " +
           "COALESCE(STRING_AGG(DISTINCT r.role_name, ','), u.user_type) as roles, " +
           "COALESCE(STRING_AGG(DISTINCT p.permission_name, ','), '') as permissions, " +
           "u.active " +
           "FROM users u " +
           "LEFT JOIN user_roles ur ON u.user_id = ur.user_id " +
           "LEFT JOIN roles r ON ur.role_id = r.role_id " +
           "LEFT JOIN role_permissions rp ON r.role_id = rp.role_id " +
           "LEFT JOIN permissions p ON rp.permission_id = p.permission_id " +
           "LEFT JOIN customers c ON u.user_id = c.user_id AND (:merchantId IS NULL OR c.merchant_id = :merchantId) " +
           "WHERE u.phone = :phone AND " +
           "(:merchantId = 0 AND (u.merchant_id IS NULL OR u.user_type IN ('merchant', 'super_admin')) OR " +
           ":merchantId > 0 AND u.merchant_id = :merchantId) " +
           "GROUP BY u.user_id, u.phone, u.password_hash, u.user_type, u.merchant_id, c.customer_id, u.active",
           nativeQuery = true)
    Optional<LoginUserData> findLoginUserData(@Param("phone") String phone, @Param("merchantId") Integer merchantId);
    
    // OPTIMIZED USERNAME LOGIN QUERY
    @Cacheable(value = "loginUserData", key = "#username + '_admin_0'", unless = "#result == null")
    @Query(value = "SELECT u.user_id, u.phone, u.password_hash, u.user_type, " +
           "CASE WHEN u.merchant_id IS NOT NULL THEN u.merchant_id ELSE 0 END, " +
           "CAST(null AS INTEGER), " +
           "COALESCE(STRING_AGG(DISTINCT r.role_name, ','), u.user_type) as roles, " +
           "COALESCE(STRING_AGG(DISTINCT p.permission_name, ','), '') as permissions, " +
           "u.active " +
           "FROM users u " +
           "LEFT JOIN user_roles ur ON u.user_id = ur.user_id " +
           "LEFT JOIN roles r ON ur.role_id = r.role_id " +
           "LEFT JOIN role_permissions rp ON r.role_id = rp.role_id " +
           "LEFT JOIN permissions p ON rp.permission_id = p.permission_id " +
           "WHERE u.username = :username AND u.user_type IN ('merchant', 'super_admin') " +
           "GROUP BY u.user_id, u.phone, u.password_hash, u.user_type, u.merchant_id, u.active",
           nativeQuery = true)
    Optional<LoginUserData> findAdminLoginUserData(@Param("username") String username);
    Optional<User> findByPhone(String phone);
    Optional<User> findByPhoneAndMerchantIsNull(String phone);
    Optional<User> findByPhoneAndMerchant_MerchantId(String phone, Integer merchantId);
    Optional<User> findByEmailAndMerchantIsNull(String email);
    Optional<User> findByEmailAndMerchant_MerchantId(String email, Integer merchantId);
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameAndMerchantIsNull(String username);
    Optional<User> findByUsernameAndMerchant_MerchantId(String username, Integer merchantId);
    Optional<User> findByMerchantAndUserType(Merchant merchant, String userType);
    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByUsernameAndMerchantIsNull(String username);
    boolean existsByUsernameAndMerchant_MerchantId(String username, Integer merchantId);
    
    // Merchant-aware existence checks
    boolean existsByPhoneAndMerchant_MerchantId(String phone, Integer merchantId);
    boolean existsByPhoneAndMerchantIsNull(String phone);
    boolean existsByEmailAndMerchant_MerchantId(String email, Integer merchantId);
    boolean existsByEmailAndMerchantIsNull(String email);
    
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.otpCode = :otpCode, u.otpExpiresAt = :expiresAt, u.otpAttempts = 0 WHERE u.phone = :phone AND (u.merchant.merchantId = :merchantId OR (:merchantId IS NULL AND u.merchant IS NULL))")
    int updateOtpByPhone(@Param("phone") String phone, @Param("otpCode") String otpCode, @Param("expiresAt") LocalDateTime expiresAt, @Param("merchantId") Integer merchantId);
    
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.otpAttempts = :attempts WHERE u.phone = :phone AND (u.merchant.merchantId = :merchantId OR (:merchantId IS NULL AND u.merchant IS NULL))")
    int updateOtpAttempts(@Param("phone") String phone, @Param("attempts") Integer attempts, @Param("merchantId") Integer merchantId);
    
    List<User> findByUserType(String userType);
    Page<User> findByUserType(String userType, Pageable pageable);
    List<User> findByUserTypeAndMerchant_MerchantId(String userType, Integer merchantId);
    Page<User> findByUserTypeAndMerchant_MerchantId(String userType, Integer merchantId, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.userType = 'customer' AND " +
           "(LOWER(u.firstName) LIKE LOWER(:search) OR " +
           "LOWER(u.lastName) LIKE LOWER(:search) OR " +
           "LOWER(u.email) LIKE LOWER(:search) OR " +
           "u.phone LIKE :search)")
    Page<User> findCustomersBySearch(@Param("search") String search, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.userType = 'customer' AND u.merchant.merchantId = :merchantId AND " +
           "(LOWER(u.firstName) LIKE LOWER(:search) OR " +
           "LOWER(u.lastName) LIKE LOWER(:search) OR " +
           "LOWER(u.email) LIKE LOWER(:search) OR " +
           "u.phone LIKE :search)")
    Page<User> findCustomersByMerchantIdAndSearch(@Param("merchantId") Integer merchantId, @Param("search") String search, Pageable pageable);
    
    @Modifying
    @Transactional
    void deleteByMerchant(Merchant merchant);

    // User Management API methods
    @Query("SELECT u FROM User u WHERE " +
           "(:role IS NULL OR u.userType = :role) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(CAST(u.firstName AS string)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(CAST(u.lastName AS string)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(CAST(u.email AS string)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(CAST(u.username AS string)) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "CAST(u.phone AS string) LIKE CONCAT('%', :search, '%'))")
    Page<User> findAllUsersWithFilters(@Param("role") String role, @Param("search") String search, Pageable pageable);
}
