package com.cloudkitchen.rbac.service;

import java.util.List;

import org.springframework.security.core.Authentication;

import com.cloudkitchen.rbac.dto.merchant.MerchantRequest;
import com.cloudkitchen.rbac.dto.merchant.MerchantResponse;

public interface MerchantService {
    MerchantResponse createMerchant(MerchantRequest request);
    MerchantResponse updateMerchant(Integer id, MerchantRequest request);
    MerchantResponse getMerchantById(Integer id);
    List<MerchantResponse> getAllMerchants();
    void deleteMerchant(Integer id);
    boolean canAccessMerchant(Authentication authentication, Integer merchantId);
}