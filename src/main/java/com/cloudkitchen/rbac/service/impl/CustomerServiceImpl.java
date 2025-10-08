package com.cloudkitchen.rbac.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;

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

    @Override
    public List<CustomerResponse> getAllCustomers(Authentication authentication) {
        try {
            Integer userId = Integer.valueOf(authentication.getName());
            User user = userRepository.findById(userId).orElse(null);
            
            if (user != null && "merchant".equals(user.getUserType()) && user.getMerchant() != null) {
                // Merchant can only see their own customers
                return getCustomersByMerchantId(user.getMerchant().getMerchantId());
            }
            
            // Super admin or users with customer.read permission see all
            return getAllCustomers();
        } catch (Exception e) {
            return getAllCustomers();
        }
    }
    
    @Override
    public boolean canAccessCustomers(Authentication authentication) {
        try {
            Integer userId = Integer.valueOf(authentication.getName());
            User user = userRepository.findById(userId).orElse(null);
            
            // Merchants can access their own customers
            return user != null && "merchant".equals(user.getUserType());
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean canAccessCustomer(Authentication authentication, Integer customerId) {
        try {
            Integer userId = Integer.valueOf(authentication.getName());
            User user = userRepository.findById(userId).orElse(null);
            User customer = userRepository.findById(customerId).orElse(null);
            
            System.out.println("DEBUG - canAccessCustomer: userId=" + userId + ", customerId=" + customerId);
            
            if (user == null) {
                System.out.println("DEBUG - User not found: " + userId);
                return false;
            }
            
            if (customer == null) {
                System.out.println("DEBUG - Customer not found: " + customerId);
                return false;
            }
            
            System.out.println("DEBUG - User type: " + user.getUserType() + ", merchantId: " + 
                             (user.getMerchant() != null ? user.getMerchant().getMerchantId() : "null"));
            System.out.println("DEBUG - Customer type: " + customer.getUserType() + ", merchantId: " + 
                             (customer.getMerchant() != null ? customer.getMerchant().getMerchantId() : "null"));
            
            // Customer can access their own data
            if (userId.equals(customerId)) {
                System.out.println("DEBUG - Customer accessing own data");
                return true;
            }
            
            // Merchant can access their own customers
            boolean canAccess = "merchant".equals(user.getUserType()) && 
                               user.getMerchant() != null && 
                               customer.getMerchant() != null &&
                               user.getMerchant().getMerchantId().equals(customer.getMerchant().getMerchantId());
            
            System.out.println("DEBUG - Merchant access result: " + canAccess);
            return canAccess;
        } catch (Exception e) {
            System.out.println("DEBUG - Exception in canAccessCustomer: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean canAccessMerchantCustomers(Authentication authentication, Integer merchantId) {
        try {
            Integer userId = Integer.valueOf(authentication.getName());
            User user = userRepository.findById(userId).orElse(null);
            
            // Merchant can access their own customers
            return user != null && 
                   "merchant".equals(user.getUserType()) && 
                   user.getMerchant() != null &&
                   merchantId.equals(user.getMerchant().getMerchantId());
        } catch (Exception e) {
            return false;
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