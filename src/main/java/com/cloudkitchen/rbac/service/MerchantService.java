package com.cloudkitchen.rbac.service;

import com.cloudkitchen.rbac.domain.entity.Merchant;
import java.util.List;

public interface MerchantService {
    Merchant addMerchant(Merchant merchant);
    Merchant updateMerchant(Integer id, Merchant merchant);
    Merchant getMerchantById(Integer id);
    List<Merchant> getAllMerchants();
    void deleteMerchant(Integer id);
}
