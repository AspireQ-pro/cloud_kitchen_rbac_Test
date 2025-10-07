package com.cloudkitchen.rbac.service.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(MerchantServiceImpl.class);
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
        if (request == null) {
            throw new IllegalArgumentException("Merchant request cannot be null");
        }
        
        try {
            if (merchantRepository.existsByPhone(request.getPhone())) {
                throw new RuntimeException("Merchant with this phone number already exists");
            }
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new RuntimeException("Username already exists");
            }
        
        // Create merchant
        Merchant merchant = new Merchant();
        merchant.setMerchantName(request.getMerchantName());
        merchant.setBusinessName(request.getMerchantName()); // Use merchantName as businessName
        merchant.setPhone(request.getPhone());
        merchant.setEmail(request.getEmail());
        merchant.setAddress(request.getAddress());
        merchant.setGstin(request.getGstin());
        merchant.setFssaiLicense(request.getFssaiLicense());
        merchant.setActive(true);
        merchant.setCreatedBy(0); // System created
        
        merchant = merchantRepository.save(merchant);
        
        // Create user account for merchant
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getMerchantName()); // Use merchant name as first name
        user.setLastName("Admin"); // Default last name
        user.setUserType("merchant");
        user.setMerchant(merchant);
        user.setActive(true);
        user.setEmailVerified(true);
        user.setPhoneVerified(true);
        user.setCreatedBy(0); // System created
        
            userRepository.save(user);
            
            log.info("Created merchant: {} with username: {}", merchant.getMerchantName(), request.getUsername());
            MerchantResponse response = mapToResponse(merchant);
            response.setUsername(request.getUsername());
            return response;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error creating merchant: {}", request.getMerchantName(), e);
            throw new RuntimeException("Failed to create merchant", e);
        }
    }

    @Override
    public MerchantResponse updateMerchant(Integer id, MerchantRequest request) {
        if (id == null) {
            throw new IllegalArgumentException("Merchant ID cannot be null");
        }
        if (request == null) {
            throw new IllegalArgumentException("Merchant request cannot be null");
        }
        
        try {
            Merchant merchant = merchantRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Merchant not found with ID: " + id));
        
        if (!merchant.getPhone().equals(request.getPhone()) && 
            merchantRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Merchant with this phone number already exists");
        }
        
        merchant.setMerchantName(request.getMerchantName());
        merchant.setPhone(request.getPhone());
        merchant.setEmail(request.getEmail());
        merchant.setAddress(request.getAddress());
        merchant.setGstin(request.getGstin());
        merchant.setFssaiLicense(request.getFssaiLicense());
        
            merchant = merchantRepository.save(merchant);
            log.info("Updated merchant: {} (ID: {})", merchant.getMerchantName(), id);
            return mapToResponse(merchant);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error updating merchant with ID: {}", id, e);
            throw new RuntimeException("Failed to update merchant", e);
        }
    }

    @Override
    public MerchantResponse getMerchantById(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("Merchant ID cannot be null");
        }
        
        try {
            Merchant merchant = merchantRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Merchant not found with ID: " + id));
            return mapToResponse(merchant);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving merchant with ID: {}", id, e);
            throw new RuntimeException("Failed to retrieve merchant", e);
        }
    }

    @Override
    public List<MerchantResponse> getAllMerchants() {
        try {
            List<Merchant> merchants = merchantRepository.findAll();
            return merchants.stream()
                    .map(this::mapToResponse)
                    .toList();
        } catch (Exception e) {
            log.error("Error retrieving all merchants", e);
            throw new RuntimeException("Failed to retrieve merchants", e);
        }
    }

    @Override
    public void deleteMerchant(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("Merchant ID cannot be null");
        }
        
        try {
            if (!merchantRepository.existsById(id)) {
                throw new RuntimeException("Merchant not found with ID: " + id);
            }
            merchantRepository.deleteById(id);
            log.info("Deleted merchant with ID: {}", id);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deleting merchant with ID: {}", id, e);
            throw new RuntimeException("Failed to delete merchant", e);
        }
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