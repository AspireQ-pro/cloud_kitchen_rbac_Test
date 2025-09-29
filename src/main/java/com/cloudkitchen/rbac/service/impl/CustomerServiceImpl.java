package com.cloudkitchen.rbac.service.impl;

import com.cloudkitchen.rbac.domain.entity.User;
import com.cloudkitchen.rbac.dto.customer.CustomerResponse;
import com.cloudkitchen.rbac.repository.UserRepository;
import com.cloudkitchen.rbac.service.CustomerService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class CustomerServiceImpl implements CustomerService {
    private final UserRepository userRepository;

    public CustomerServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<CustomerResponse> getAllCustomers(Integer requestingUserId) {
        User requestingUser = getUser(requestingUserId);
        
        // Only super_admin can see all customers
        if (!"super_admin".equals(requestingUser.getUserType())) {
            throw new RuntimeException("Access denied");
        }
        
        return userRepository.findByUserType("customer").stream()
                .map(this::mapToCustomerResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CustomerResponse> getCustomersByMerchantId(Integer merchantId, Integer requestingUserId) {
        User requestingUser = getUser(requestingUserId);
        
        // Super admin can see any merchant's customers
        if ("super_admin".equals(requestingUser.getUserType())) {
            return userRepository.findByUserTypeAndMerchant_MerchantId("customer", merchantId).stream()
                    .map(this::mapToCustomerResponse)
                    .collect(Collectors.toList());
        }
        
        // Merchant can only see their own customers
        if ("merchant".equals(requestingUser.getUserType())) {
            Integer userMerchantId = requestingUser.getMerchant() != null ? requestingUser.getMerchant().getMerchantId() : null;
            if (!merchantId.equals(userMerchantId)) {
                throw new RuntimeException("Access denied");
            }
            return userRepository.findByUserTypeAndMerchant_MerchantId("customer", merchantId).stream()
                    .map(this::mapToCustomerResponse)
                    .collect(Collectors.toList());
        }
        
        throw new RuntimeException("Access denied");
    }
    
    @Override
    public CustomerResponse getCustomerById(Integer customerId, Integer requestingUserId) {
        User requestingUser = getUser(requestingUserId);
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        
        if (!"customer".equals(customer.getUserType())) {
            throw new IllegalArgumentException("User is not a customer");
        }
        
        // Super admin can see any customer
        if ("super_admin".equals(requestingUser.getUserType())) {
            return mapToCustomerResponse(customer);
        }
        
        // Merchant can see their customers
        if ("merchant".equals(requestingUser.getUserType())) {
            Integer userMerchantId = requestingUser.getMerchant() != null ? requestingUser.getMerchant().getMerchantId() : null;
            Integer customerMerchantId = customer.getMerchant() != null ? customer.getMerchant().getMerchantId() : null;
            if (!Objects.equals(userMerchantId, customerMerchantId)) {
                throw new RuntimeException("Access denied");
            }
            return mapToCustomerResponse(customer);
        }
        
        // Customer can only see their own profile
        if ("customer".equals(requestingUser.getUserType())) {
            if (!customerId.equals(requestingUserId)) {
                throw new RuntimeException("Access denied");
            }
            return mapToCustomerResponse(customer);
        }
        
        throw new RuntimeException("Access denied");
    }
    
    @Override
    public CustomerResponse updateCustomer(Integer customerId, CustomerResponse req, Integer requestingUserId) {
        User requestingUser = getUser(requestingUserId);
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        
        if (!"customer".equals(customer.getUserType())) {
            throw new IllegalArgumentException("User is not a customer");
        }
        
        // Super admin can update any customer
        if ("super_admin".equals(requestingUser.getUserType())) {
            updateCustomerFields(customer, req);
            userRepository.save(customer);
            return mapToCustomerResponse(customer);
        }
        
        // Customer can only update their own profile
        if ("customer".equals(requestingUser.getUserType())) {
            if (!customerId.equals(requestingUserId)) {
                throw new RuntimeException("Access denied");
            }
            updateCustomerFields(customer, req);
            userRepository.save(customer);
            return mapToCustomerResponse(customer);
        }
        
        throw new RuntimeException("Access denied");
    }
    
    private User getUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
    
    private void updateCustomerFields(User customer, CustomerResponse req) {
        if (req.getFirstName() != null) customer.setFirstName(req.getFirstName());
        if (req.getLastName() != null) customer.setLastName(req.getLastName());
        if (req.getEmail() != null) customer.setEmail(req.getEmail());
        if (req.getAddress() != null) customer.setAddress(req.getAddress());
    }

    private CustomerResponse mapToCustomerResponse(User user) {
        CustomerResponse response = new CustomerResponse();
        response.setUserId(user.getUserId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setPhone(user.getPhone());
        response.setEmail(user.getEmail());
        response.setAddress(user.getAddress());
        if (user.getMerchant() != null) {
            response.setMerchantId(user.getMerchant().getMerchantId());
            response.setMerchantName(user.getMerchant().getMerchantName());
        }
        return response;
    }
}