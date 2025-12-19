package com.cloudkitchen.rbac.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cloudkitchen.rbac.domain.entity.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    
    Optional<Customer> findByPhoneAndMerchant_MerchantId(String phone, Integer merchantId);
    
    Optional<Customer> findByUser_UserIdAndMerchant_MerchantId(Integer userId, Integer merchantId);
    
    boolean existsByPhoneAndMerchant_MerchantId(String phone, Integer merchantId);
}
