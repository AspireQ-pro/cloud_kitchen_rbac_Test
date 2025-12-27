package com.cloudkitchen.rbac.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.cloudkitchen.rbac.domain.entity.Customer;
import com.cloudkitchen.rbac.dto.common.PageRequest;
import com.cloudkitchen.rbac.dto.common.PageResponse;
import com.cloudkitchen.rbac.dto.customer.CustomerResponse;
import com.cloudkitchen.rbac.dto.customer.CustomerUpdateRequest;
import com.cloudkitchen.rbac.exception.BusinessExceptions.AccessDeniedException;
import com.cloudkitchen.rbac.exception.BusinessExceptions.CustomerNotFoundException;
import com.cloudkitchen.rbac.repository.CustomerRepository;
import com.cloudkitchen.rbac.security.JwtAuthenticationDetails;
import com.cloudkitchen.rbac.service.CloudStorageService;
import com.cloudkitchen.rbac.service.CustomerService;
import com.cloudkitchen.rbac.util.AccessControlUtil;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final AccessControlUtil accessControlUtil;
    private final CloudStorageService cloudStorageService;
    
    public CustomerServiceImpl(CustomerRepository customerRepository, AccessControlUtil accessControlUtil, CloudStorageService cloudStorageService) {
        this.customerRepository = customerRepository;
        this.accessControlUtil = accessControlUtil;
        this.cloudStorageService = cloudStorageService;
    }

    @Override
    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAllByDeletedAtIsNull().stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    public List<CustomerResponse> getAllCustomers(Authentication authentication) {
        if (accessControlUtil.isSuperAdmin(authentication)) {
            return getAllCustomers();
        } else if (accessControlUtil.isMerchant(authentication)) {
            Integer merchantId = getMerchantIdFromAuth(authentication);
            return getCustomersByMerchantId(merchantId);
        }
        throw new RuntimeException("Access denied");
    }

    @Override
    public PageResponse<CustomerResponse> getAllCustomers(PageRequest pageRequest) {
        return getAllCustomers(pageRequest, null, null);
    }

    @Override
    public PageResponse<CustomerResponse> getAllCustomers(PageRequest pageRequest, String status, String search) {
        Pageable pageable = createPageable(pageRequest);
        Page<Customer> customerPage;

        if (search != null && !search.trim().isEmpty()) {
            String searchTerm = "%" + search.trim().toLowerCase() + "%";
            customerPage = customerRepository.findBySearchAndDeletedAtIsNull(searchTerm, pageable);
        } else if (status != null && !status.trim().isEmpty()) {
            boolean active = "active".equalsIgnoreCase(status.trim());
            customerPage = customerRepository.findByIsActiveAndDeletedAtIsNull(active, pageable);
        } else {
            customerPage = customerRepository.findByDeletedAtIsNull(pageable);
        }

        List<CustomerResponse> content = customerPage.getContent().stream()
                .map(this::convertToResponse)
                .toList();

        return new PageResponse<>(
                content,
                customerPage.getNumber(),
                customerPage.getSize(),
                customerPage.getTotalElements()
        );
    }

    @Override
    public PageResponse<CustomerResponse> getAllCustomers(PageRequest pageRequest, String status, String search, Authentication authentication) {
        if (accessControlUtil.isSuperAdmin(authentication)) {
            return getAllCustomersWithFilters(pageRequest);
        } else if (accessControlUtil.isMerchant(authentication)) {
            Integer merchantId = getMerchantIdFromAuth(authentication);
            return getCustomersByMerchantIdWithFilters(merchantId, pageRequest);
        } else if (accessControlUtil.isCustomer(authentication)) {
            throw new RuntimeException("Customers cannot access customer lists");
        }
        throw new RuntimeException("Access denied");
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponse> getCustomersByMerchantId(Integer merchantId) {
        return customerRepository.findByMerchant_MerchantIdAndDeletedAtIsNull(merchantId).stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> getCustomersByMerchantId(Integer merchantId, PageRequest pageRequest) {
        return getCustomersByMerchantIdWithFilters(merchantId, pageRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Integer id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + id));
        return convertToResponse(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Integer id, Authentication authentication) {
        if (accessControlUtil.isSuperAdmin(authentication)) {
            return getCustomerById(id);
        }
        if (accessControlUtil.isCustomer(authentication)) {
            Integer userId = getCustomerIdFromAuth(authentication);
            if (userId == null) {
                throw new AccessDeniedException("Access denied");
            }
            Customer customer = customerRepository
                    .findByCustomerIdAndUser_UserIdAndDeletedAtIsNull(id, userId)
                    .orElseThrow(() -> new AccessDeniedException("Access denied"));
            return convertToResponse(customer);
        }
        if (accessControlUtil.isMerchant(authentication)) {
            Integer merchantId = getMerchantIdFromAuth(authentication);
            if (merchantId == null) {
                throw new AccessDeniedException("Access denied");
            }
            Customer customer = customerRepository
                    .findByCustomerIdAndMerchant_MerchantIdAndDeletedAtIsNull(id, merchantId)
                    .orElseThrow(() -> new AccessDeniedException("Access denied"));
            return convertToResponse(customer);
        }
        throw new AccessDeniedException("Access denied");
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerProfile(Authentication authentication) {
        Integer userId = getCustomerIdFromAuth(authentication);
        Integer merchantId = getMerchantIdFromAuth(authentication);
        Customer customer = customerRepository.findByUser_UserIdAndMerchant_MerchantId(userId, merchantId)
                .orElseThrow(() -> new RuntimeException("Customer profile not found"));
        return convertToResponse(customer);
    }

    @Override
    public CustomerResponse updateCustomer(Integer id, CustomerUpdateRequest request, Integer updatedBy) {
        return updateCustomer(id, request, null, updatedBy);
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomer(Integer id, CustomerUpdateRequest request, MultipartFile profileImage, Integer updatedBy) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
        
        // Update customer fields if request is provided
        if (request != null) {
            if (request.getFirstName() != null) customer.setFirstName(request.getFirstName());
            if (request.getLastName() != null) customer.setLastName(request.getLastName());
            if (request.getEmail() != null) customer.setEmail(request.getEmail());
            if (request.getAddress() != null) customer.setAddress(request.getAddress());
            if (request.getCity() != null) customer.setCity(request.getCity());
            if (request.getState() != null) customer.setState(request.getState());
            if (request.getCountry() != null) customer.setCountry(request.getCountry());
            if (request.getPincode() != null) customer.setPincode(request.getPincode());
            if (request.getDob() != null) customer.setDob(request.getDob());
            if (request.getFavoriteFood() != null) customer.setFavoriteFood(request.getFavoriteFood());
        }
        
        // Handle profile image upload if provided
        if (profileImage != null && !profileImage.isEmpty()) {
            String imageKey = uploadProfileImage(customer, profileImage);
            customer.setProfileImageUrl(imageKey);
        }
        
        customer.setUpdatedBy(updatedBy);
        customer.setUpdatedOn(LocalDateTime.now());
        Customer updatedCustomer = customerRepository.save(customer);
        return convertToResponse(updatedCustomer);
    }

    @Override
    public CustomerResponse updateCustomerProfile(Authentication authentication, CustomerUpdateRequest request) {
        Integer userId = getCustomerIdFromAuth(authentication);
        Integer merchantId = getMerchantIdFromAuth(authentication);
        Customer customer = customerRepository.findByUser_UserIdAndMerchant_MerchantId(userId, merchantId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer profile not found"));
        return updateCustomer(customer.getCustomerId(), request, userId);
    }

    @Override
    public void deleteCustomer(Integer id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + id));
        customer.setDeletedAt(LocalDateTime.now());
        customerRepository.save(customer);
    }

    @Override
    public boolean canAccessCustomers(Authentication authentication) {
        return accessControlUtil.isSuperAdmin(authentication) || accessControlUtil.isMerchant(authentication);
    }

    @Override
    public boolean canAccessCustomer(Authentication authentication, Integer customerId) {
        if (accessControlUtil.isSuperAdmin(authentication)) {
            return true;
        }
        if (accessControlUtil.isCustomer(authentication)) {
            // For customers, check if the customerId belongs to their userId
            Integer userId = getCustomerIdFromAuth(authentication);
            if (userId == null) {
                return false;
            }
            return customerRepository.existsByCustomerIdAndUser_UserIdAndDeletedAtIsNull(customerId, userId);
        }
        if (accessControlUtil.isMerchant(authentication)) {
            Customer customer = customerRepository.findById(customerId).orElse(null);
            return customer != null && getMerchantIdFromAuth(authentication).equals(customer.getMerchant().getMerchantId());
        }
        return false;
    }

    @Override
    public boolean canAccessMerchantCustomers(Authentication authentication, Integer merchantId) {
        if (accessControlUtil.isSuperAdmin(authentication)) {
            return true;
        }
        if (accessControlUtil.isMerchant(authentication)) {
            return merchantId.equals(getMerchantIdFromAuth(authentication));
        }
        // Customers should not be able to access merchant customer lists
        // They can only access their own profile through individual customer endpoints
        return false;
    }

    @Override
    public Integer getMerchantIdFromAuth(Authentication authentication) {
        if (authentication == null) {
            return null;
        }

        // Extract merchantId from JWT authentication details
        if (authentication.getDetails() instanceof JwtAuthenticationDetails) {
            JwtAuthenticationDetails details = (JwtAuthenticationDetails) authentication.getDetails();
            return details.getMerchantId();
        }

        return null;
    }

    @Override
    public Integer getCustomerIdFromAuth(Authentication authentication) {
        return accessControlUtil.getUserId(authentication);
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomer(Integer id, CustomerUpdateRequest request, MultipartFile profileImage, Authentication authentication) {
        Integer updatedBy = authentication != null ? Integer.valueOf(authentication.getName()) : null;
        return updateCustomer(id, request, profileImage, updatedBy);
    }

    @Override
    public PageResponse<CustomerResponse> getAllCustomers(int page, int size, String status, String search, Authentication authentication) {
        PageRequest pageRequest = new PageRequest(page, size);
        return getAllCustomers(pageRequest, status, search, authentication);
    }

    @Override
    public PageResponse<CustomerResponse> getCustomersByMerchantId(Integer merchantId, int page, int size, Authentication authentication) {
        PageRequest pageRequest = new PageRequest(page, size);
        return getCustomersByMerchantId(merchantId, pageRequest);
    }

    @Transactional(readOnly = true)
    private PageResponse<CustomerResponse> getAllCustomersWithFilters(PageRequest pageRequest) {
        Pageable pageable = createPageable(pageRequest);
        Page<Customer> customerPage = customerRepository.findByDeletedAtIsNull(pageable);

        List<CustomerResponse> responses = customerPage.getContent().stream()
                .map(this::convertToResponse)
                .toList();

        return new PageResponse<>(
                responses,
                customerPage.getNumber(),
                customerPage.getSize(),
                customerPage.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    private PageResponse<CustomerResponse> getCustomersByMerchantIdWithFilters(Integer merchantId, PageRequest pageRequest) {
        Pageable pageable = createPageable(pageRequest);
        Page<Customer> customerPage = customerRepository.findByMerchant_MerchantIdAndDeletedAtIsNull(merchantId, pageable);

        List<CustomerResponse> responses = customerPage.getContent().stream()
                .map(this::convertToResponse)
                .toList();

        return new PageResponse<>(
                responses,
                customerPage.getNumber(),
                customerPage.getSize(),
                customerPage.getTotalElements()
        );
    }

    private Pageable createPageable(PageRequest pageRequest) {
        String sortBy = pageRequest.getSortBy();
        if (sortBy != null && !sortBy.trim().isEmpty()) {
            Sort sort = "desc".equalsIgnoreCase(pageRequest.getSortDirection())
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();
            return org.springframework.data.domain.PageRequest.of(pageRequest.getPage(), pageRequest.getSize(), sort);
        }
        return org.springframework.data.domain.PageRequest.of(pageRequest.getPage(), pageRequest.getSize());
    }

    private CustomerResponse convertToResponse(Customer customer) {
        CustomerResponse response = new CustomerResponse();
        response.setId(customer.getCustomerId());
        response.setFirstName(customer.getFirstName());
        response.setLastName(customer.getLastName());
        response.setPhone(customer.getPhone());
        response.setEmail(customer.getEmail());
        response.setAddress(customer.getAddress());
        response.setCity(customer.getCity());
        response.setState(customer.getState());
        response.setCountry(customer.getCountry());
        response.setPincode(customer.getPincode());
        response.setDob(customer.getDob());
        response.setFavoriteFood(customer.getFavoriteFood());
        
        // Generate presigned URL if S3 key exists
        if (customer.getProfileImageUrl() != null) {
            String presignedUrl = cloudStorageService.generatePresignedUrl(customer.getProfileImageUrl());
            response.setProfileImageUrl(presignedUrl);
        }
        
        response.setActive(customer.getIsActive());
        response.setCreatedAt(customer.getCreatedOn());
        response.setUpdatedAt(customer.getUpdatedOn());
        
        if (customer.getMerchant() != null) {
            response.setMerchantId(customer.getMerchant().getMerchantId());
            response.setMerchantName(customer.getMerchant().getBusinessName());
        }
        
        return response;
    }
    
    private String uploadProfileImage(Customer customer, MultipartFile profileImage) {
        // Validate image
        validateProfileImage(profileImage);
        
        try {
            // Get file extension
            String originalFilename = profileImage.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            
            // Build S3 key: {merchantId}/customer/{customerId}/profile_img/profile.{extension}
            String merchantId = customer.getMerchant().getMerchantId().toString();
            String customerId = customer.getCustomerId().toString();
            String s3Key = merchantId + "/customer/" + customerId + "/profile_img/profile" + extension;
            
            // Upload to S3
            cloudStorageService.uploadFile(
                s3Key,
                profileImage.getInputStream(),
                profileImage.getSize(),
                profileImage.getContentType()
            );
            
            return s3Key;
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload profile image: " + e.getMessage(), e);
        }
    }
    
    private void validateProfileImage(MultipartFile profileImage) {
        if (profileImage == null || profileImage.isEmpty()) {
            throw new IllegalArgumentException("Profile image cannot be empty");
        }
        
        // Check file size (2MB limit)
        long maxSize = 2 * 1024 * 1024; // 2MB
        if (profileImage.getSize() > maxSize) {
            throw new IllegalArgumentException("Profile image size cannot exceed 2MB");
        }
        
        // Check content type
        String contentType = profileImage.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && 
                                   !contentType.equals("image/jpg") && 
                                   !contentType.equals("image/png"))) {
            throw new IllegalArgumentException("Only JPG, JPEG, and PNG images are allowed");
        }
        
        // Check file extension
        String filename = profileImage.getOriginalFilename();
        if (filename != null) {
            String extension = filename.toLowerCase();
            if (!extension.endsWith(".jpg") && !extension.endsWith(".jpeg") && !extension.endsWith(".png")) {
                throw new IllegalArgumentException("Only JPG, JPEG, and PNG files are allowed");
            }
        }
    }
}
