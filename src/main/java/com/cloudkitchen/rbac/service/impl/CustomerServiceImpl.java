package com.cloudkitchen.rbac.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.cloudkitchen.rbac.domain.entity.Customer;
import com.cloudkitchen.rbac.dto.common.PageRequest;
import com.cloudkitchen.rbac.dto.common.PageResponse;
import com.cloudkitchen.rbac.dto.customer.CustomerResponse;
import com.cloudkitchen.rbac.dto.customer.CustomerUpdateRequest;
import com.cloudkitchen.rbac.repository.CustomerRepository;
import com.cloudkitchen.rbac.service.CustomerService;
import com.cloudkitchen.rbac.util.AccessControlUtil;

@Service
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final AccessControlUtil accessControlUtil;
    
    public CustomerServiceImpl(CustomerRepository customerRepository, AccessControlUtil accessControlUtil) {
        this.customerRepository = customerRepository;
        this.accessControlUtil = accessControlUtil;
    }

    @Override
    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAllByDeletedAtIsNull().stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    public List<CustomerResponse> getAllCustomers(Authentication authentication) {
        if (accessControlUtil.isSuperAdmin(authentication)) {
            return getAllCustomers();
        } else if (accessControlUtil.isMerchant(authentication)) {
            Integer merchantId = getMerchantIdFromAuth(authentication);
            return getCustomersByMerchantId(merchantId);
        }
        throw new RuntimeException("Access denied");
    }

    @Override
    public PageResponse<CustomerResponse> getAllCustomers(PageRequest pageRequest) {
        return null;
    }

    @Override
    public PageResponse<CustomerResponse> getAllCustomers(PageRequest pageRequest, String status, String search) {
        return null;
    }

    @Override
    public PageResponse<CustomerResponse> getAllCustomers(PageRequest pageRequest, String status, String search, Authentication authentication) {
        if (accessControlUtil.isSuperAdmin(authentication)) {
            return getAllCustomersWithFilters(pageRequest);
        } else if (accessControlUtil.isMerchant(authentication)) {
            Integer merchantId = getMerchantIdFromAuth(authentication);
            return getCustomersByMerchantIdWithFilters(merchantId, pageRequest);
        }
        throw new RuntimeException("Access denied");
    }

    @Override
    public List<CustomerResponse> getCustomersByMerchantId(Integer merchantId) {
        return customerRepository.findByMerchant_MerchantIdAndDeletedAtIsNull(merchantId).stream()
                .map(this::convertToResponse)
                .toList();
    }

    @Override
    public PageResponse<CustomerResponse> getCustomersByMerchantId(Integer merchantId, PageRequest pageRequest) {
        return getCustomersByMerchantIdWithFilters(merchantId, pageRequest);
    }

    @Override
    public CustomerResponse getCustomerById(Integer id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
        return convertToResponse(customer);
    }

    @Override
    public CustomerResponse getCustomerProfile(Authentication authentication) {
        Integer customerId = getCustomerIdFromAuth(authentication);
        return getCustomerById(customerId);
    }

    @Override
    public CustomerResponse updateCustomer(Integer id, CustomerUpdateRequest request, Integer updatedBy) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
        
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
        
        customer.setUpdatedBy(updatedBy);
        customer.setUpdatedOn(LocalDateTime.now());
        Customer updatedCustomer = customerRepository.save(customer);
        return convertToResponse(updatedCustomer);
    }

    @Override
    public CustomerResponse updateCustomerProfile(Authentication authentication, CustomerUpdateRequest request) {
        Integer customerId = getCustomerIdFromAuth(authentication);
        return updateCustomer(customerId, request, customerId);
    }

    @Override
    public void deleteCustomer(Integer id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
        customer.setDeletedAt(LocalDateTime.now());
        customerRepository.save(customer);
    }

    @Override
    public boolean canAccessCustomers(Authentication authentication) {
        return accessControlUtil.isSuperAdmin(authentication) || accessControlUtil.isMerchant(authentication);
    }

    @Override
    public boolean canAccessCustomer(Authentication authentication, Integer customerId) {
        if (accessControlUtil.isSuperAdmin(authentication)) {
            return true;
        }
        if (accessControlUtil.isCustomer(authentication)) {
            return customerId.equals(getCustomerIdFromAuth(authentication));
        }
        if (accessControlUtil.isMerchant(authentication)) {
            Customer customer = customerRepository.findById(customerId).orElse(null);
            return customer != null && getMerchantIdFromAuth(authentication).equals(customer.getMerchant().getMerchantId());
        }
        return false;
    }

    @Override
    public boolean canAccessMerchantCustomers(Authentication authentication, Integer merchantId) {
        if (accessControlUtil.isSuperAdmin(authentication)) {
            return true;
        }
        if (accessControlUtil.isMerchant(authentication)) {
            return merchantId.equals(getMerchantIdFromAuth(authentication));
        }
        return false;
    }

    @Override
    public Integer getMerchantIdFromAuth(Authentication authentication) {
        return accessControlUtil.getUserId(authentication);
    }

    @Override
    public Integer getCustomerIdFromAuth(Authentication authentication) {
        return accessControlUtil.getUserId(authentication);
    }

    private PageResponse<CustomerResponse> getAllCustomersWithFilters(PageRequest pageRequest) {
        List<Customer> customers = customerRepository.findAllByDeletedAtIsNull();
        List<CustomerResponse> responses = customers.stream()
                .map(this::convertToResponse)
                .toList();
        
        return new PageResponse<>(responses, pageRequest.getPage(), pageRequest.getSize(), responses.size());
    }

    private PageResponse<CustomerResponse> getCustomersByMerchantIdWithFilters(Integer merchantId, PageRequest pageRequest) {
        List<Customer> customers = customerRepository.findByMerchant_MerchantIdAndDeletedAtIsNull(merchantId);
        List<CustomerResponse> responses = customers.stream()
                .map(this::convertToResponse)
                .toList();
        
        return new PageResponse<>(responses, pageRequest.getPage(), pageRequest.getSize(), responses.size());
    }

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
        response.setActive(customer.getIsActive());
        response.setCreatedAt(customer.getCreatedOn());
        response.setUpdatedAt(customer.getUpdatedOn());
        
        if (customer.getMerchant() != null) {
            response.setMerchantId(customer.getMerchant().getMerchantId());
            response.setMerchantName(customer.getMerchant().getBusinessName());
        }
        
        return response;
    }
}