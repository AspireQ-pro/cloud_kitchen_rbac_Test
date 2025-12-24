package com.cloudkitchen.rbac.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

import com.cloudkitchen.rbac.domain.entity.Merchant;
import com.cloudkitchen.rbac.domain.entity.User;
import com.cloudkitchen.rbac.dto.common.PageRequest;
import com.cloudkitchen.rbac.dto.common.PageResponse;
import com.cloudkitchen.rbac.dto.merchant.MerchantRequest;
import com.cloudkitchen.rbac.dto.merchant.MerchantResponse;
import com.cloudkitchen.rbac.repository.MerchantRepository;
import com.cloudkitchen.rbac.repository.UserRepository;
import com.cloudkitchen.rbac.service.MerchantService;
import com.cloudkitchen.rbac.service.CloudStorageService;
import com.cloudkitchen.rbac.service.ValidationService;
import com.cloudkitchen.rbac.exception.BusinessExceptions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@Transactional
public class MerchantServiceImpl implements MerchantService {
    private static final Logger log = LoggerFactory.getLogger(MerchantServiceImpl.class);
    private final MerchantRepository merchantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ValidationService validationService;

    @Autowired(required = false)
    private CloudStorageService cloudStorageService;

    public MerchantServiceImpl(MerchantRepository merchantRepository, UserRepository userRepository,
                              PasswordEncoder passwordEncoder, ValidationService validationService) {
        this.merchantRepository = merchantRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.validationService = validationService;
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

        Merchant savedMerchant = merchantRepository.save(merchant);

        // Create user account for merchant
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getMerchantName()); // Use merchant name as first name
        user.setLastName("Admin"); // Default last name
        user.setUserType("merchant");
        user.setMerchant(savedMerchant);
        user.setActive(true);
        user.setEmailVerified(true);
        user.setPhoneVerified(true);
        user.setCreatedBy(0); // System created

        userRepository.save(user);

        // Create S3 folder structure for merchant (async - non-blocking)
        if (cloudStorageService != null) {
            final Integer merchantId = savedMerchant.getMerchantId();
            cloudStorageService.createMerchantFolderStructureAsync(merchantId.toString())
                .exceptionally(ex -> {
                    log.warn("Async S3 folder creation failed for merchant {}: {}", merchantId, ex.getMessage());
                    return null;
                });
        }

        MerchantResponse response = mapToResponse(savedMerchant);
        response.setUsername(request.getUsername());
        return response;
    }

    @Override
    public MerchantResponse updateMerchant(Integer id, MerchantRequest request) {
        // 1. VALIDATE REQUEST BODY - Check for empty request
        if (request == null || isEmptyRequest(request)) {
            throw new ValidationException("All required fields are missing");
        }

        // 2. VALIDATE REQUIRED FIELDS - merchantName is mandatory
        validationService.validateMerchantName(request.getMerchantName());

        // 3. VALIDATE FIELD FORMATS - Fail-fast validation
        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            validationService.validateEmail(request.getEmail());
        }

        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            validationService.validatePhone(request.getPhone());
        }

        if (request.getGstin() != null && !request.getGstin().trim().isEmpty()) {
            validationService.validateGstin(request.getGstin());
        }

        if (request.getFssaiLicense() != null && !request.getFssaiLicense().trim().isEmpty()) {
            validationService.validateFssaiLicense(request.getFssaiLicense());
        }

        if (request.getAddress() != null && !request.getAddress().trim().isEmpty()) {
            validationService.validateAddress(request.getAddress());
        }

        // 4. PASSWORD POLICY - If password is provided
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            validationService.validatePassword(request.getPassword());
        }

        // 5. FETCH MERCHANT - Must exist
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new MerchantNotFoundException("Merchant with ID " + id + " not found."));

        // 6. BUSINESS RULE VALIDATION - Username uniqueness
        if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
            User existingMerchantUser = userRepository.findByMerchantAndUserType(merchant, "merchant")
                    .orElse(null);

            if (existingMerchantUser != null && !existingMerchantUser.getUsername().equals(request.getUsername())) {
                if (userRepository.existsByUsername(request.getUsername())) {
                    throw new UserAlreadyExistsException("Username '" + request.getUsername() + "' already exists");
                }
                existingMerchantUser.setUsername(request.getUsername());
                userRepository.save(existingMerchantUser);
            }
        }

        // 7. PHONE UNIQUENESS CHECK
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            if (!merchant.getPhone().equals(request.getPhone()) &&
                merchantRepository.existsByPhone(request.getPhone())) {
                throw new MerchantAlreadyExistsException("A merchant with phone number " + maskPhone(request.getPhone()) + " already exists. Please use a different phone number.");
            }
        }

        // 8. UPDATE LOGIC - Only after all validations pass
        merchant.setMerchantName(request.getMerchantName());

        if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
            merchant.setEmail(request.getEmail());
        }

        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            merchant.setPhone(request.getPhone());
        }

        if (request.getAddress() != null && !request.getAddress().trim().isEmpty()) {
            merchant.setAddress(request.getAddress());
        }

        if (request.getGstin() != null && !request.getGstin().trim().isEmpty()) {
            merchant.setGstin(request.getGstin());
        }

        if (request.getFssaiLicense() != null && !request.getFssaiLicense().trim().isEmpty()) {
            merchant.setFssaiLicense(request.getFssaiLicense());
        }

        // Update password for associated merchant user if provided
        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            userRepository.findByMerchantAndUserType(merchant, "merchant")
                    .ifPresent(user -> {
                        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
                        userRepository.save(user);
                    });
        }

        merchant = merchantRepository.save(merchant);
        return mapToResponse(merchant);
    }

    private boolean isEmptyRequest(MerchantRequest request) {
        return (request.getMerchantName() == null || request.getMerchantName().trim().isEmpty()) &&
               (request.getEmail() == null || request.getEmail().trim().isEmpty()) &&
               (request.getPhone() == null || request.getPhone().trim().isEmpty()) &&
               (request.getAddress() == null || request.getAddress().trim().isEmpty()) &&
               (request.getGstin() == null || request.getGstin().trim().isEmpty()) &&
               (request.getFssaiLicense() == null || request.getFssaiLicense().trim().isEmpty()) &&
               (request.getUsername() == null || request.getUsername().trim().isEmpty()) &&
               (request.getPassword() == null || request.getPassword().trim().isEmpty());
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
    public PageResponse<MerchantResponse> getAllMerchants(PageRequest pageRequest) {
        return getAllMerchants(pageRequest, null, null);
    }

    @Override
    public PageResponse<MerchantResponse> getAllMerchants(PageRequest pageRequest, String status, String search) {
        Pageable pageable = createPageable(pageRequest);
        Page<Merchant> merchantPage;

        if (search != null && !search.trim().isEmpty()) {
            // Search by name, email, or phone
            String searchTerm = "%" + search.trim().toLowerCase() + "%";
            merchantPage = merchantRepository.findByMerchantNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrPhoneContaining(
                    searchTerm, searchTerm, searchTerm, pageable);
        } else if (status != null && !status.trim().isEmpty()) {
            boolean active = "active".equalsIgnoreCase(status.trim());
            merchantPage = merchantRepository.findByActive(active, pageable);
        } else {
            merchantPage = merchantRepository.findAll(pageable);
        }

        // Optimized: Fetch all merchants with their users in a single query to avoid N+1 problem
        List<Integer> merchantIds = merchantPage.getContent().stream()
                .map(Merchant::getMerchantId)
                .collect(Collectors.toList());

        // Fetch merchants with users eagerly loaded
        List<Merchant> merchantsWithUsers = merchantIds.isEmpty() ?
                List.of() :
                merchantRepository.findByIdInWithUsers(merchantIds);

        // Create a map for quick lookup
        java.util.Map<Integer, Merchant> merchantMap = merchantsWithUsers.stream()
                .collect(Collectors.toMap(Merchant::getMerchantId, m -> m));

        // Map to response DTOs using the pre-fetched data
        List<MerchantResponse> content = merchantPage.getContent().stream()
                .map(merchant -> mapToResponseOptimized(merchantMap.getOrDefault(merchant.getMerchantId(), merchant)))
                .collect(Collectors.toList());

        return new PageResponse<>(
                content,
                merchantPage.getNumber(),
                merchantPage.getSize(),
                merchantPage.getTotalElements()
        );
    }

    private MerchantResponse mapToResponseOptimized(Merchant merchant) {
        MerchantResponse response = new MerchantResponse();
        response.setMerchantId(merchant.getMerchantId());
        response.setMerchantName(merchant.getMerchantName());
        response.setPhone(merchant.getPhone());
        response.setEmail(merchant.getEmail());
        response.setAddress(merchant.getAddress());
        response.setGstin(merchant.getGstin() != null ? merchant.getGstin() : "");
        response.setFssaiLicense(merchant.getFssaiLicense() != null ? merchant.getFssaiLicense() : "");
        response.setActive(merchant.getActive());
        response.setCreatedAt(merchant.getCreatedOn());
        response.setUpdatedAt(merchant.getUpdatedOn());

        // Find the merchant admin user from pre-fetched users collection
        if (merchant.getUsers() != null) {
            merchant.getUsers().stream()
                    .filter(user -> "merchant".equals(user.getUserType()))
                    .findFirst()
                    .ifPresent(user -> {
                        response.setUsername(user.getUsername());
                        response.setUserId(user.getUserId());
                    });
        }

        return response;
    }

    private Pageable createPageable(PageRequest pageRequest) {
        Sort sort = Sort.unsorted();
        if (pageRequest.getSortBy() != null && !pageRequest.getSortBy().trim().isEmpty()) {
            Sort.Direction direction = "desc".equalsIgnoreCase(pageRequest.getSortDirection()) 
                    ? Sort.Direction.DESC 
                    : Sort.Direction.ASC;
            sort = Sort.by(direction, pageRequest.getSortBy());
        }
        return org.springframework.data.domain.PageRequest.of(pageRequest.getPage(), pageRequest.getSize(), sort);
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
        response.setMerchantId(merchant.getMerchantId());
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