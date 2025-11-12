package com.cloudkitchen.rbac.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

import com.cloudkitchen.rbac.domain.entity.Merchant;
import com.cloudkitchen.rbac.domain.entity.User;
import com.cloudkitchen.rbac.dto.merchant.MerchantRequest;
import com.cloudkitchen.rbac.dto.merchant.MerchantResponse;
import com.cloudkitchen.rbac.repository.MerchantRepository;
import com.cloudkitchen.rbac.repository.UserRepository;
import com.cloudkitchen.rbac.service.MerchantService;
import com.cloudkitchen.rbac.service.CloudStorageService;
import com.cloudkitchen.rbac.exception.BusinessExceptions.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@Transactional
public class MerchantServiceImpl implements MerchantService {
    private static final Logger log = LoggerFactory.getLogger(MerchantServiceImpl.class);
    private final MerchantRepository merchantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CloudStorageService cloudStorageService;

    public MerchantServiceImpl(MerchantRepository merchantRepository, UserRepository userRepository,
                              PasswordEncoder passwordEncoder, CloudStorageService cloudStorageService) {
        this.merchantRepository = merchantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.cloudStorageService = cloudStorageService;
    }

    @Override
    public MerchantResponse createMerchant(MerchantRequest request) {
        if (merchantRepository.existsByPhone(request.getPhone())) {
            throw new MerchantAlreadyExistsException("A merchant with phone number " + maskPhone(request.getPhone()) + " already exists. Please use a different phone number.");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("Username '" + request.getUsername() + "' is already taken. Please choose a different username.");
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
        
        // Create S3 folder structure for merchant (non-blocking)
        try {
            cloudStorageService.createMerchantFolderStructure(merchant.getMerchantId().toString());
        } catch (Exception e) {
            // Log error but don't fail merchant creation
            log.warn("Failed to create S3 folders for merchant {}: {}", merchant.getMerchantId(), e.getMessage());
        }
        
        MerchantResponse response = mapToResponse(merchant);
        response.setUsername(request.getUsername());
        return response;
    }

    @Override
    public MerchantResponse updateMerchant(Integer id, MerchantRequest request) {
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new MerchantNotFoundException("Merchant with ID " + id + " not found."));
        
        if (!merchant.getPhone().equals(request.getPhone()) && 
            merchantRepository.existsByPhone(request.getPhone())) {
            throw new MerchantAlreadyExistsException("A merchant with phone number " + maskPhone(request.getPhone()) + " already exists. Please use a different phone number.");
        }
        
        merchant.setMerchantName(request.getMerchantName());
        merchant.setPhone(request.getPhone());
        merchant.setEmail(request.getEmail());
        merchant.setAddress(request.getAddress());
        merchant.setGstin(request.getGstin());
        merchant.setFssaiLicense(request.getFssaiLicense());
        
        merchant = merchantRepository.save(merchant);
        return mapToResponse(merchant);
    }

    @Override
    public MerchantResponse getMerchantById(Integer id) {
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new MerchantNotFoundException("Merchant with ID " + id + " not found."));
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
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new MerchantNotFoundException("Merchant with ID " + id + " not found."));
        
        // Delete associated users first to avoid foreign key constraint issues
        userRepository.deleteByMerchant(merchant);
        
        // Then delete the merchant
        merchantRepository.deleteById(id);
    }

    @Override
    public boolean canAccessMerchant(Authentication authentication, Integer merchantId) {
        if (authentication == null || merchantId == null) {
            return false;
        }
        
        try {
            Integer userId = Integer.valueOf(authentication.getName());
            User user = userRepository.findById(userId).orElse(null);
            
            if (user == null) {
                return false;
            }
            
            // Only merchant admins can access merchant data, not customers
            return "merchant".equals(user.getUserType()) && 
                   user.getMerchant() != null &&
                   merchantId.equals(user.getMerchant().getMerchantId());
        } catch (Exception e) {
            return false;
        }
    }
    
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return "****" + phone.substring(phone.length() - 4);
    }

    private MerchantResponse mapToResponse(Merchant merchant) {
        MerchantResponse response = new MerchantResponse();
        response.setId(merchant.getMerchantId());
        response.setMerchantName(merchant.getMerchantName());
        response.setPhone(merchant.getPhone());
        response.setEmail(merchant.getEmail());
        response.setAddress(merchant.getAddress());
        response.setGstin(merchant.getGstin() != null ? merchant.getGstin() : "");
        response.setFssaiLicense(merchant.getFssaiLicense() != null ? merchant.getFssaiLicense() : "");
        response.setActive(merchant.getActive());
        response.setCreatedAt(merchant.getCreatedOn());
        response.setUpdatedAt(merchant.getUpdatedOn());
        
        // Find the merchant admin user to get username and userId
        try {
            userRepository.findByMerchantAndUserType(merchant, "merchant")
                    .ifPresent(user -> {
                        response.setUsername(user.getUsername());
                        response.setUserId(user.getUserId());
                    });
        } catch (Exception e) {
            log.warn("Error fetching merchant admin user for merchant {}: {}", merchant.getMerchantId(), e.getMessage());
        }
        
        return response;
    }
}