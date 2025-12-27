package com.cloudkitchen.rbac.service;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import com.cloudkitchen.rbac.dto.common.PageRequest;
import com.cloudkitchen.rbac.dto.common.PageResponse;
import com.cloudkitchen.rbac.dto.customer.CustomerResponse;
import com.cloudkitchen.rbac.dto.customer.CustomerUpdateRequest;

public interface CustomerService {
    // Read operations
    List<CustomerResponse> getAllCustomers();
    List<CustomerResponse> getAllCustomers(Authentication authentication);
    PageResponse<CustomerResponse> getAllCustomers(PageRequest pageRequest);
    PageResponse<CustomerResponse> getAllCustomers(PageRequest pageRequest, String status, String search);
    PageResponse<CustomerResponse> getAllCustomers(PageRequest pageRequest, String status, String search, Authentication authentication);
    List<CustomerResponse> getCustomersByMerchantId(Integer merchantId);
    PageResponse<CustomerResponse> getCustomersByMerchantId(Integer merchantId, PageRequest pageRequest);
    CustomerResponse getCustomerById(Integer id);
    CustomerResponse getCustomerById(Integer id, Authentication authentication);
    CustomerResponse getCustomerProfile(Authentication authentication);
    
    // Update operations
    CustomerResponse updateCustomer(Integer id, CustomerUpdateRequest request, Integer updatedBy);
    CustomerResponse updateCustomer(Integer id, CustomerUpdateRequest request, MultipartFile profileImage, Integer updatedBy);
    CustomerResponse updateCustomerProfile(Authentication authentication, CustomerUpdateRequest request);
    
    // Delete operations
    void deleteCustomer(Integer id);
    
    // Access control methods
    boolean canAccessCustomers(Authentication authentication);
    boolean canAccessCustomer(Authentication authentication, Integer customerId);
    boolean canAccessMerchantCustomers(Authentication authentication, Integer merchantId);
    Integer getMerchantIdFromAuth(Authentication authentication);
    Integer getCustomerIdFromAuth(Authentication authentication);
}
