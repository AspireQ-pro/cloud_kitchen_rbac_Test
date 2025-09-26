package com.cloudkitchen.rbac.service;

import com.cloudkitchen.rbac.dto.customer.CustomerResponse;
import java.util.List;

public interface CustomerService {
    List<CustomerResponse> getAllCustomers(Integer requestingUserId);
    List<CustomerResponse> getCustomersByMerchantId(Integer merchantId, Integer requestingUserId);
    CustomerResponse getCustomerById(Integer customerId, Integer requestingUserId);
    CustomerResponse updateCustomer(Integer customerId, CustomerResponse req, Integer requestingUserId);
}