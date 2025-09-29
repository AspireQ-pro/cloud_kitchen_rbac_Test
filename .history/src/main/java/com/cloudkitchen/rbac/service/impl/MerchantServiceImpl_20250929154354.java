package com.cloudkitchen.rbac.service.impl;
import com.cloudkitchen.rbac.domain.entity.Merchant;
import com.cloudkitchen.rbac.repository.MerchantRepository;
import com.cloudkitchen.rbac.service.MerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MerchantServiceImpl implements MerchantService {
    @Autowired
    private MerchantRepository merchantRepository;

    @Override
    public Merchant createMerchant(com.cloudkitchen.rbac.dto.merchant.MerchantRequest req) {
        Merchant merchant = new Merchant();
        merchant.setMerchantName(req.getMerchantName());
        merchant.setBusinessName(req.getBusinessName());
        merchant.setBusinessType(req.getBusinessType());
        merchant.setWebsiteUrl(req.getWebsiteUrl());
        merchant.setPhone(req.getPhone());
        merchant.setEmail(req.getEmail());
        merchant.setAddress(req.getAddress());
        merchant.setCity(req.getCity());
        merchant.setState(req.getState());
        merchant.setCountry(req.getCountry());
        merchant.setPincode(req.getPincode());
        merchant.setActive(req.getActive());
        merchant.setSubscriptionPlan(req.getSubscriptionPlan());
        merchant.setSubscriptionExpiresAt(req.getSubscriptionExpiresAt());
        return merchantRepository.save(merchant);
    }

    @Override
    public Merchant updateMerchant(Integer id, com.cloudkitchen.rbac.dto.merchant.MerchantRequest req) {
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));
        merchant.setMerchantName(req.getMerchantName());
        merchant.setBusinessName(req.getBusinessName());
        merchant.setBusinessType(req.getBusinessType());
        merchant.setWebsiteUrl(req.getWebsiteUrl());
        merchant.setPhone(req.getPhone());
        merchant.setEmail(req.getEmail());
        merchant.setAddress(req.getAddress());
        merchant.setCity(req.getCity());
        merchant.setState(req.getState());
        merchant.setCountry(req.getCountry());
        merchant.setPincode(req.getPincode());
        merchant.setActive(req.getActive());
        merchant.setSubscriptionPlan(req.getSubscriptionPlan());
        merchant.setSubscriptionExpiresAt(req.getSubscriptionExpiresAt());
        return merchantRepository.save(merchant);
    }
    


    @Override
    public Merchant getMerchantById(Integer id) {
        return merchantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));
    }

    @Override
    public List<Merchant> getAllMerchants() {
        return merchantRepository.findAll();
    }

    @Override
    public void deleteMerchant(Integer id) {
        merchantRepository.deleteById(id);
    }
}
