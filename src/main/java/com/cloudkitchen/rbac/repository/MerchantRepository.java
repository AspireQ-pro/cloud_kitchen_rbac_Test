package com.cloudkitchen.rbac.repository;

import com.cloudkitchen.rbac.domain.entity.Merchant;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, Integer> {
    @Cacheable(value = "merchantData", key = "'email_' + #email")
    Optional<Merchant> findByEmail(String email);
    
    @Cacheable(value = "merchantData", key = "'phone_' + #phone")
    Optional<Merchant> findByPhone(String phone);
    
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
}
