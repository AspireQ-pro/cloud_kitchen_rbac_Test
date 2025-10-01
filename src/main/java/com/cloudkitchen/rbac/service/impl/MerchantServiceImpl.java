package com.cloudkitchen.rbac.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cloudkitchen.rbac.domain.entity.Merchant;
import com.cloudkitchen.rbac.domain.entity.User;
import com.cloudkitchen.rbac.dto.merchant.MerchantRequest;
import com.cloudkitchen.rbac.dto.merchant.MerchantResponse;
import com.cloudkitchen.rbac.repository.MerchantRepository;
import com.cloudkitchen.rbac.repository.UserRepository;
import com.cloudkitchen.rbac.service.MerchantService;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@Transactional
public class MerchantServiceImpl implements MerchantService {
    private final MerchantRepository merchantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public MerchantServiceImpl(MerchantRepository merchantRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.merchantRepository = merchantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public MerchantResponse createMerchant(MerchantRequest request) {
        if (merchantRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Merchant with this phone number already exists");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        
        // Create merchant
        Merchant merchant = new Merchant();
        merchant.setMerchantName(request.getMerchantName());
        merchant.setPhone(request.getPhone());
        merchant.setEmail(request.getEmail());
        merchant.setAddress(request.getAddress());
        merchant.setActive(true);
        
        merchant = merchantRepository.save(merchant);
        
        // Create user account for merchant
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setUserType("merchant");
        user.setMerchant(merchant);
        user.setActive(true);
        
        userRepository.save(user);
        
        MerchantResponse response = mapToResponse(merchant);
        response.setUsername(request.getUsername());
        return response;
    }

    @Override
    public MerchantResponse updateMerchant(Integer id, MerchantRequest request) {
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));
        
        if (!merchant.getPhone().equals(request.getPhone()) && 
            merchantRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Merchant with this phone number already exists");
        }
        
        merchant.setMerchantName(request.getMerchantName());
        merchant.setPhone(request.getPhone());
        merchant.setEmail(request.getEmail());
        merchant.setAddress(request.getAddress());
        
        merchant = merchantRepository.save(merchant);
        return mapToResponse(merchant);
    }

    @Override
    public MerchantResponse getMerchantById(Integer id) {
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));
        return mapToResponse(merchant);
    }

    @Override
    public List<MerchantResponse> getAllMerchants() {
        return merchantRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteMerchant(Integer id) {
        if (!merchantRepository.existsById(id)) {
            throw new RuntimeException("Merchant not found");
        }
        merchantRepository.deleteById(id);
    }

    private MerchantResponse mapToResponse(Merchant merchant) {
        MerchantResponse response = new MerchantResponse();
        response.setId(merchant.getMerchantId());
        response.setMerchantName(merchant.getMerchantName());
        response.setPhone(merchant.getPhone());
        response.setEmail(merchant.getEmail());
        response.setAddress(merchant.getAddress());
        response.setActive(merchant.getActive());
        response.setCreatedAt(merchant.getCreatedOn());
        response.setUpdatedAt(merchant.getUpdatedOn());
        return response;
    }
}