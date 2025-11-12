package com.cloudkitchen.rbac.service;

import java.util.List;

import org.springframework.security.core.Authentication;

import com.cloudkitchen.rbac.dto.customer.CustomerResponse;

public interface CustomerService {
    List<CustomerResponse> getAllCustomers();
    List<CustomerResponse> getAllCustomers(Authentication authentication);
    List<CustomerResponse> getCustomersByMerchantId(Integer merchantId);
    CustomerResponse getCustomerById(Integer id);
    boolean canAccessCustomers(Authentication authentication);
    boolean canAccessCustomer(Authentication authentication, Integer customerId);
    boolean canAccessMerchantCustomers(Authentication authentication, Integer merchantId);
    Integer getMerchantIdFromAuth(Authentication authentication);
}