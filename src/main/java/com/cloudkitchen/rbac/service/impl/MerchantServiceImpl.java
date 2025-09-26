package com.cloudkitchen.rbac.service.impl;

import com.cloudkitchen.rbac.domain.entity.Merchant;
import com.cloudkitchen.rbac.repository.MerchantRepository;
import com.cloudkitchen.rbac.service.MerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MerchantServiceImpl implements MerchantService {
    @Autowired
    private MerchantRepository merchantRepository;

    @Override
    public Merchant createMerchant(com.cloudkitchen.rbac.dto.merchant.MerchantRequest req) {
        Merchant merchant = new Merchant();
        merchant.setMerchantName(req.getMerchantName());
        merchant.setAddress(req.getAddress());
        merchant.setPhone(req.getPhone());
        merchant.setEmail(req.getEmail());
        return merchantRepository.save(merchant);
    }

    @Override
    public Merchant updateMerchant(Integer id, com.cloudkitchen.rbac.dto.merchant.MerchantRequest req) {
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));
        merchant.setMerchantName(req.getMerchantName());
        merchant.setAddress(req.getAddress());
        merchant.setPhone(req.getPhone());
        merchant.setEmail(req.getEmail());
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
