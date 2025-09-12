package com.cloudkitchen.rbac.repository;

import com.cloudkitchen.rbac.domain.entity.User;
import com.cloudkitchen.rbac.domain.entity.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;
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
}