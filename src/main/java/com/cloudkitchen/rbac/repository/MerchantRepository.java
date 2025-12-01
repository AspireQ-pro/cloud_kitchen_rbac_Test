package com.cloudkitchen.rbac.repository;

import com.cloudkitchen.rbac.domain.entity.Merchant;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, Integer> {
    @Cacheable(value = "merchantData", key = "'email_' + #email")
    Optional<Merchant> findByEmail(String email);
    
    @Cacheable(value = "merchantData", key = "'phone_' + #phone")
    Optional<Merchant> findByPhone(String phone);
    
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    
    Page<Merchant> findByActive(boolean active, Pageable pageable);
    
    @Query("SELECT m FROM Merchant m WHERE " +
           "LOWER(m.merchantName) LIKE LOWER(:search) OR " +
           "LOWER(m.email) LIKE LOWER(:search) OR " +
           "m.phone LIKE :search")
    Page<Merchant> findByMerchantNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrPhoneContaining(
            @Param("search") String merchantName,
            @Param("search") String email,
            @Param("search") String phone,
            Pageable pageable);
}
