package com.cloudkitchen.rbac.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cloudkitchen.rbac.domain.entity.MerchantWebsiteConfig;

@Repository
public interface MerchantWebsiteConfigRepository extends JpaRepository<MerchantWebsiteConfig, Long> {
    
    Optional<MerchantWebsiteConfig> findByMerchant_MerchantId(Integer merchantId);
    
    boolean existsByWebsiteAddress(String websiteAddress);
    
    boolean existsByWebsiteAddressAndMerchant_MerchantIdNot(String websiteAddress, Integer merchantId);
}
