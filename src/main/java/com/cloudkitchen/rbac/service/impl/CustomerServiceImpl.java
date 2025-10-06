package com.cloudkitchen.rbac.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cloudkitchen.rbac.domain.entity.User;
import com.cloudkitchen.rbac.dto.customer.CustomerResponse;
import com.cloudkitchen.rbac.repository.UserRepository;
import com.cloudkitchen.rbac.service.CustomerService;

@Service
@Transactional(readOnly = true)
public class CustomerServiceImpl implements CustomerService {
    private final UserRepository userRepository;

    public CustomerServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<CustomerResponse> getAllCustomers() {
        return userRepository.findByUserType("customer").stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerResponse> getCustomersByMerchantId(Integer merchantId) {
        return userRepository.findByUserTypeAndMerchant_MerchantId("customer", merchantId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CustomerResponse getCustomerById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        
        if (!"customer".equals(user.getUserType())) {
            throw new RuntimeException("User is not a customer");
        }
        
        return mapToResponse(user);
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