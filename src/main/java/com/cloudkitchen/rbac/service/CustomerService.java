package com.cloudkitchen.rbac.service;

import java.util.List;

import org.springframework.security.core.Authentication;

import com.cloudkitchen.rbac.dto.common.PageRequest;
import com.cloudkitchen.rbac.dto.common.PageResponse;
import com.cloudkitchen.rbac.dto.customer.CustomerResponse;

public interface CustomerService {
    List<CustomerResponse> getAllCustomers();
    List<CustomerResponse> getAllCustomers(Authentication authentication);
    PageResponse<CustomerResponse> getAllCustomers(PageRequest pageRequest);
    PageResponse<CustomerResponse> getAllCustomers(PageRequest pageRequest, String status, String search);
    List<CustomerResponse> getCustomersByMerchantId(Integer merchantId);
    PageResponse<CustomerResponse> getCustomersByMerchantId(Integer merchantId, PageRequest pageRequest);
    CustomerResponse getCustomerById(Integer id);
    void deleteCustomer(Integer id);
    boolean canAccessCustomers(Authentication authentication);
    boolean canAccessCustomer(Authentication authentication, Integer customerId);
    boolean canAccessMerchantCustomers(Authentication authentication, Integer merchantId);
    Integer getMerchantIdFromAuth(Authentication authentication);
}