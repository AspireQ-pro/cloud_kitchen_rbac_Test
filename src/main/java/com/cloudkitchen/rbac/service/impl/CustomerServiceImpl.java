package com.cloudkitchen.rbac.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.cloudkitchen.rbac.constants.ResponseMessages;
import com.cloudkitchen.rbac.domain.entity.Customer;
import com.cloudkitchen.rbac.dto.common.PageRequest;
import com.cloudkitchen.rbac.dto.common.PageResponse;
import com.cloudkitchen.rbac.dto.customer.CustomerResponse;
import com.cloudkitchen.rbac.dto.customer.CustomerUpdateRequest;
import com.cloudkitchen.rbac.exception.BusinessExceptions.AccessDeniedException;
import com.cloudkitchen.rbac.exception.BusinessExceptions.CustomerNotFoundException;
import com.cloudkitchen.rbac.exception.BusinessExceptions.FileUploadException;
import com.cloudkitchen.rbac.repository.CustomerRepository;
import com.cloudkitchen.rbac.security.JwtAuthenticationDetails;
import com.cloudkitchen.rbac.service.CloudStorageService;
import com.cloudkitchen.rbac.service.CustomerService;
import com.cloudkitchen.rbac.util.AccessControlUtil;
import com.cloudkitchen.rbac.util.HttpResponseUtil;
import com.cloudkitchen.rbac.util.ResponseBuilder;

/**
 * Service implementation for customer operations, including access checks,
 * pagination, and response assembly for controllers.
 */
@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final AccessControlUtil accessControlUtil;
    private final CloudStorageService cloudStorageService;
    
    /**
     * Construct the customer service with required repositories and helpers.
     */
    public CustomerServiceImpl(CustomerRepository customerRepository, AccessControlUtil accessControlUtil, CloudStorageService cloudStorageService) {
        this.customerRepository = customerRepository;
        this.accessControlUtil = accessControlUtil;
        this.cloudStorageService = cloudStorageService;
    }

    /**
     * Return all active customers without access filtering.
     */
    @Override
    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAllByDeletedAtIsNull().stream()
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * Return customers visible to the authenticated user.
     */
    @Override
    public List<CustomerResponse> getAllCustomers(Authentication authentication) {
        if (accessControlUtil.isSuperAdmin(authentication)) {
            return getAllCustomers();
        } else if (accessControlUtil.isMerchant(authentication)) {
            Integer merchantId = getMerchantIdFromAuth(authentication);
            return getCustomersByMerchantId(merchantId);
        }
        throw new AccessDeniedException("Access denied");
    }

    /**
     * Return paginated customers without filters.
     */
    @Override
    public PageResponse<CustomerResponse> getAllCustomers(PageRequest pageRequest) {
        return getAllCustomers(pageRequest, null, null);
    }

    /**
     * Return paginated customers with optional status/search filters.
     */
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

    /**
     * Return paginated customers visible to the authenticated user.
     */
    @Override
    public PageResponse<CustomerResponse> getAllCustomers(PageRequest pageRequest, String status, String search, Authentication authentication) {
        if (accessControlUtil.isSuperAdmin(authentication)) {
            return getAllCustomersWithFilters(pageRequest);
        } else if (accessControlUtil.isMerchant(authentication)) {
            Integer merchantId = getMerchantIdFromAuth(authentication);
            return getCustomersByMerchantIdWithFilters(merchantId, pageRequest);
        } else if (accessControlUtil.isCustomer(authentication)) {
            throw new AccessDeniedException("Customers cannot access customer lists");
        }
        throw new AccessDeniedException("Access denied");
    }

    /**
     * Return active customers for a specific merchant.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponse> getCustomersByMerchantId(Integer merchantId) {
        return customerRepository.findByMerchant_MerchantIdAndDeletedAtIsNull(merchantId).stream()
                .map(this::convertToResponse)
                .toList();
    }

    /**
     * Return paginated customers for a specific merchant.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<CustomerResponse> getCustomersByMerchantId(Integer merchantId, PageRequest pageRequest) {
        return getCustomersByMerchantIdWithFilters(merchantId, pageRequest);
    }

    /**
     * Fetch a customer by ID or throw if not found.
     */
    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(Integer id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + id));
        return convertToResponse(customer);
    }

    /**
     * Fetch a customer by ID with access control checks.
     */
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

    /**
     * Fetch the profile for the authenticated customer.
     */
    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomerProfile(Authentication authentication) {
        Integer userId = getCustomerIdFromAuth(authentication);
        Integer merchantId = getMerchantIdFromAuth(authentication);
        Customer customer = customerRepository.findByUser_UserIdAndMerchant_MerchantId(userId, merchantId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer profile not found"));
        return convertToResponse(customer);
    }

    /**
     * Update customer fields without a profile image.
     */
    @Override
    public CustomerResponse updateCustomer(Integer id, CustomerUpdateRequest request, Integer updatedBy) {
        return updateCustomer(id, request, null, updatedBy);
    }

    /**
     * Update customer fields and optionally update the profile image.
     */
    @Override
    @Transactional
    public CustomerResponse updateCustomer(Integer id, CustomerUpdateRequest request, MultipartFile profileImage, Integer updatedBy) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + id));
        
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

    /**
     * Update the authenticated customer's own profile.
     */
    @Override
    public CustomerResponse updateCustomerProfile(Authentication authentication, CustomerUpdateRequest request) {
        Integer userId = getCustomerIdFromAuth(authentication);
        Integer merchantId = getMerchantIdFromAuth(authentication);
        Customer customer = customerRepository.findByUser_UserIdAndMerchant_MerchantId(userId, merchantId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer profile not found"));
        return updateCustomer(customer.getCustomerId(), request, userId);
    }

    /**
     * Soft-delete a customer by ID.
     */
    @Override
    public void deleteCustomer(Integer id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with id: " + id));
        customer.setDeletedAt(LocalDateTime.now());
        customerRepository.save(customer);
    }

    /**
     * Check if the authenticated user can list customers.
     */
    @Override
    public boolean canAccessCustomers(Authentication authentication) {
        return accessControlUtil.isSuperAdmin(authentication) || accessControlUtil.isMerchant(authentication);
    }

    /**
     * Check if the authenticated user can access a specific customer.
     */
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

    /**
     * Check if the authenticated user can access customers for a merchant.
     */
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

    /**
     * Extract merchant ID from authentication details.
     */
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

    /**
     * Extract customer user ID from authentication.
     */
    @Override
    public Integer getCustomerIdFromAuth(Authentication authentication) {
        return accessControlUtil.getUserId(authentication);
    }

    /**
     * Update a customer using authentication to populate updatedBy.
     */
    @Override
    @Transactional
    public CustomerResponse updateCustomer(Integer id, CustomerUpdateRequest request, MultipartFile profileImage, Authentication authentication) {
        Integer updatedBy = authentication != null ? Integer.valueOf(authentication.getName()) : null;
        return updateCustomer(id, request, profileImage, updatedBy);
    }

    /**
     * Return paginated customers using primitive pagination parameters.
     */
    @Override
    public PageResponse<CustomerResponse> getAllCustomers(int page, int size, String status, String search, Authentication authentication) {
        PageRequest pageRequest = new PageRequest(page, size);
        return getAllCustomers(pageRequest, status, search, authentication);
    }

    /**
     * Return paginated customers for a merchant using primitive pagination parameters.
     */
    @Override
    public PageResponse<CustomerResponse> getCustomersByMerchantId(Integer merchantId, int page, int size, Authentication authentication) {
        PageRequest pageRequest = new PageRequest(page, size);
        return getCustomersByMerchantId(merchantId, pageRequest);
    }

    /**
     * Build the HTTP response for customer update with access checks.
     */
    @Override
    @Transactional
    public ResponseEntity<Object> updateCustomerResponse(Integer id, CustomerUpdateRequest request, MultipartFile profileImage, Authentication authentication) {
        if (!canAccessCustomer(authentication, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseBuilder.error(HttpResponseUtil.FORBIDDEN, ResponseMessages.Customer.ACCESS_DENIED_PROFILE));
        }
        try {
            CustomerResponse response = updateCustomer(id, request, profileImage, authentication);
            return ResponseEntity.ok(ResponseBuilder.success(HttpResponseUtil.OK, ResponseMessages.Customer.UPDATED_SUCCESS, response));
        } catch (CustomerNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseBuilder.error(HttpResponseUtil.NOT_FOUND, ResponseMessages.Customer.NOT_FOUND));
        } catch (IllegalArgumentException | FileUploadException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseBuilder.error(HttpResponseUtil.BAD_REQUEST, ResponseMessages.Customer.UPDATE_FAILED + ": " + e.getMessage()));
        }
    }

    /**
     * Build the HTTP response for listing customers with access checks.
     */
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<Object> getAllCustomersResponse(int page, int size, String status, String search, Authentication authentication) {
        if (!canAccessCustomers(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseBuilder.error(HttpResponseUtil.FORBIDDEN, ResponseMessages.Customer.ACCESS_DENIED_LIST));
        }
        PageResponse<CustomerResponse> customers = getAllCustomers(page, size, status, search, authentication);
        return ResponseEntity.ok(ResponseBuilder.success(HttpResponseUtil.OK, ResponseMessages.Customer.LIST_SUCCESS, customers));
    }

    /**
     * Build the HTTP response for fetching a customer by ID.
     */
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<Object> getCustomerByIdResponse(Integer id, Authentication authentication) {
        try {
            CustomerResponse customer = getCustomerById(id, authentication);
            return ResponseEntity.ok(ResponseBuilder.success(HttpResponseUtil.OK, ResponseMessages.Customer.RETRIEVED_SUCCESS, customer));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseBuilder.error(HttpResponseUtil.FORBIDDEN, ResponseMessages.Customer.ACCESS_DENIED_VIEW));
        } catch (CustomerNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseBuilder.error(HttpResponseUtil.NOT_FOUND, ResponseMessages.Customer.NOT_FOUND));
        }
    }

    /**
     * Build the HTTP response for fetching the authenticated customer's profile.
     */
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<Object> getCustomerProfileResponse(Authentication authentication) {
        try {
            CustomerResponse customer = getCustomerProfile(authentication);
            return ResponseEntity.ok(ResponseBuilder.success(HttpResponseUtil.OK, ResponseMessages.Customer.PROFILE_RETRIEVED_SUCCESS, customer));
        } catch (CustomerNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseBuilder.error(HttpResponseUtil.NOT_FOUND, ResponseMessages.Customer.PROFILE_NOT_FOUND));
        }
    }

    /**
     * Build the HTTP response for listing customers for a merchant.
     */
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<Object> getCustomersByMerchantIdResponse(Integer merchantId, int page, int size, Authentication authentication) {
        if (!canAccessMerchantCustomers(authentication, merchantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseBuilder.error(HttpResponseUtil.FORBIDDEN, ResponseMessages.Customer.ACCESS_DENIED_MERCHANT_CUSTOMERS));
        }
        PageResponse<CustomerResponse> customers = getCustomersByMerchantId(merchantId, page, size, authentication);
        return ResponseEntity.ok(ResponseBuilder.success(HttpResponseUtil.OK, ResponseMessages.Customer.MERCHANT_CUSTOMERS_SUCCESS, customers));
    }

    /**
     * Build the HTTP response for customer deletion.
     */
    @Override
    @Transactional
    public ResponseEntity<Object> deleteCustomerResponse(Integer id, Authentication authentication) {
        if (!canAccessCustomer(authentication, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseBuilder.error(HttpResponseUtil.FORBIDDEN, ResponseMessages.Customer.ACCESS_DENIED_VIEW));
        }
        try {
            deleteCustomer(id);
            return ResponseEntity.ok(ResponseBuilder.success(HttpResponseUtil.OK, ResponseMessages.Customer.DELETED_SUCCESS, null));
        } catch (CustomerNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseBuilder.error(HttpResponseUtil.NOT_FOUND, ResponseMessages.Customer.NOT_FOUND));
        }
    }

    /**
     * Apply pagination to all customers without status/search filtering.
     */
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

    /**
     * Apply pagination to customers for a merchant.
     */
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

    /**
     * Build a pageable instance with optional sorting.
     */
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

    /**
     * Map a customer entity to its response DTO.
     */
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
    
    /**
     * Validate and upload a profile image, returning the storage key.
     */
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
            throw new FileUploadException("Failed to upload profile image: " + e.getMessage());
        }
    }
    
    /**
     * Validate image size, type, and extension for profile uploads.
     */
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
