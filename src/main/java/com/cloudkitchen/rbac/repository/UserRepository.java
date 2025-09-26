package com.cloudkitchen.rbac.repository;

import com.cloudkitchen.rbac.domain.entity.User;
import com.cloudkitchen.rbac.domain.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByPhoneAndMerchantIsNull(String phone);
    Optional<User> findByPhoneAndMerchant_MerchantId(String phone, Integer merchantId);
    Optional<User> findByEmailAndMerchantIsNull(String email);
    Optional<User> findByEmailAndMerchant_MerchantId(String email, Integer merchantId);
    Optional<User> findByUsername(String username);
    Optional<User> findByMerchantAndUserType(Merchant merchant, String userType);
    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.otpCode = :otpCode, u.otpExpiresAt = :expiresAt, u.otpAttempts = 0 WHERE u.phone = :phone AND (u.merchant.merchantId = :merchantId OR (:merchantId IS NULL AND u.merchant IS NULL))")
    int updateOtpByPhone(@Param("phone") String phone, @Param("otpCode") String otpCode, @Param("expiresAt") LocalDateTime expiresAt, @Param("merchantId") Integer merchantId);
    
    @Modifying
    @Transactional
    @Query("UPDATE User u SET u.otpAttempts = :attempts WHERE u.phone = :phone AND (u.merchant.merchantId = :merchantId OR (:merchantId IS NULL AND u.merchant IS NULL))")
    int updateOtpAttempts(@Param("phone") String phone, @Param("attempts") Integer attempts, @Param("merchantId") Integer merchantId);
    
    List<User> findByUserType(String userType);
    List<User> findByUserTypeAndMerchant_MerchantId(String userType, Integer merchantId);
}