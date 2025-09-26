package com.cloudkitchen.rbac.service;

import com.cloudkitchen.rbac.domain.entity.Merchant;
import com.cloudkitchen.rbac.dto.merchant.MerchantRequest;
import java.util.List;

public interface MerchantService {
    Merchant createMerchant(MerchantRequest req);
    Merchant updateMerchant(Integer id, MerchantRequest req);
    Merchant getMerchantById(Integer id);
    List<Merchant> getAllMerchants();
    void deleteMerchant(Integer id);
}
