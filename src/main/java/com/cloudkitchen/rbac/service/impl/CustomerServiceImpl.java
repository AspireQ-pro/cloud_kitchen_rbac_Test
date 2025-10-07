package com.cloudkitchen.rbac.service.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cloudkitchen.rbac.domain.entity.User;
import com.cloudkitchen.rbac.dto.customer.CustomerResponse;
import com.cloudkitchen.rbac.repository.UserRepository;
import com.cloudkitchen.rbac.service.CustomerService;

@Service
@Transactional(readOnly = true)
public class CustomerServiceImpl implements CustomerService {
    private static final Logger log = LoggerFactory.getLogger(CustomerServiceImpl.class);
    private final UserRepository userRepository;

    public CustomerServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<CustomerResponse> getAllCustomers() {
        try {
            List<User> customers = userRepository.findByUserType("customer");
            return customers.stream()
                    .map(this::mapToResponse)
                    .toList();
        } catch (Exception e) {
            log.error("Error retrieving all customers", e);
            throw new RuntimeException("Failed to retrieve customers", e);
        }
    }

    @Override
    public List<CustomerResponse> getCustomersByMerchantId(Integer merchantId) {
        if (merchantId == null) {
            throw new IllegalArgumentException("Merchant ID cannot be null");
        }
        
        try {
            List<User> customers = userRepository.findByUserTypeAndMerchant_MerchantId("customer", merchantId);
            return customers.stream()
                    .map(this::mapToResponse)
                    .toList();
        } catch (Exception e) {
            log.error("Error retrieving customers for merchant ID: {}", merchantId, e);
            throw new RuntimeException("Failed to retrieve customers for merchant", e);
        }
    }

    @Override
    public CustomerResponse getCustomerById(Integer id) {
        if (id == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }
        
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + id));
            
            if (!"customer".equals(user.getUserType())) {
                throw new RuntimeException("User with ID " + id + " is not a customer");
            }
            
            return mapToResponse(user);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving customer with ID: {}", id, e);
            throw new RuntimeException("Failed to retrieve customer", e);
        }
    }

    private CustomerResponse mapToResponse(User user) {
        CustomerResponse response = new CustomerResponse();
        response.setId(user.getUserId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setPhone(user.getPhone());
        response.setEmail(user.getEmail());
        response.setAddress(user.getAddress());
        response.setMerchantId(user.getMerchant() != null ? user.getMerchant().getMerchantId() : null);
        response.setMerchantName(user.getMerchant() != null ? user.getMerchant().getMerchantName() : null);
        response.setActive(user.getActive());
        response.setCreatedAt(user.getCreatedOn());
        return response;
    }
}