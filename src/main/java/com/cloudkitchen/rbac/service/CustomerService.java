package com.cloudkitchen.rbac.service;

import java.util.List;

import com.cloudkitchen.rbac.dto.customer.CustomerResponse;

public interface CustomerService {
    List<CustomerResponse> getAllCustomers();
    List<CustomerResponse> getCustomersByMerchantId(Integer merchantId);
}