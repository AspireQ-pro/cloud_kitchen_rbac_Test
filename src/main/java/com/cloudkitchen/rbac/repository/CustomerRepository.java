package com.cloudkitchen.rbac.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.cloudkitchen.rbac.domain.entity.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Integer> {
    
    Optional<Customer> findByPhoneAndMerchant_MerchantId(String phone, Integer merchantId);
    
    Optional<Customer> findByUser_UserIdAndMerchant_MerchantId(Integer userId, Integer merchantId);
    
    boolean existsByPhoneAndMerchant_MerchantId(String phone, Integer merchantId);
    
    @Query("SELECT c FROM Customer c LEFT JOIN FETCH c.merchant WHERE c.merchant.merchantId = :merchantId AND c.deletedAt IS NULL")
    List<Customer> findByMerchant_MerchantIdAndDeletedAtIsNull(@Param("merchantId") Integer merchantId);

    @Query("SELECT c FROM Customer c LEFT JOIN FETCH c.merchant WHERE c.merchant.merchantId = :merchantId AND c.deletedAt IS NULL")
    Page<Customer> findByMerchant_MerchantIdAndDeletedAtIsNull(@Param("merchantId") Integer merchantId, Pageable pageable);

    @Query("SELECT c FROM Customer c LEFT JOIN FETCH c.merchant WHERE c.deletedAt IS NULL")
    Page<Customer> findByDeletedAtIsNull(Pageable pageable);

    @Query("SELECT c FROM Customer c LEFT JOIN FETCH c.merchant WHERE c.isActive = :isActive AND c.deletedAt IS NULL")
    Page<Customer> findByIsActiveAndDeletedAtIsNull(@Param("isActive") Boolean isActive, Pageable pageable);
    
    @Query("SELECT c FROM Customer c LEFT JOIN FETCH c.merchant WHERE c.deletedAt IS NULL AND " +
           "(LOWER(c.firstName) LIKE LOWER(:search) OR " +
           "LOWER(c.lastName) LIKE LOWER(:search) OR " +
           "c.phone LIKE :search OR " +
           "LOWER(c.email) LIKE LOWER(:search))")
    Page<Customer> findBySearchAndDeletedAtIsNull(@Param("search") String search, Pageable pageable);

    @Query("SELECT c FROM Customer c LEFT JOIN FETCH c.merchant WHERE c.merchant.merchantId = :merchantId AND c.deletedAt IS NULL AND " +
           "(LOWER(c.firstName) LIKE LOWER(:search) OR " +
           "LOWER(c.lastName) LIKE LOWER(:search) OR " +
           "c.phone LIKE :search OR " +
           "LOWER(c.email) LIKE LOWER(:search))")
    Page<Customer> findByMerchantIdAndSearchAndDeletedAtIsNull(@Param("merchantId") Integer merchantId,
                                                              @Param("search") String search,
                                                              Pageable pageable);

    // Additional methods for service implementation
    @Query("SELECT c FROM Customer c LEFT JOIN FETCH c.merchant WHERE c.deletedAt IS NULL")
    List<Customer> findAllByDeletedAtIsNull();
    
    boolean existsByCustomerIdAndUser_UserIdAndDeletedAtIsNull(Integer customerId, Integer userId);

    Optional<Customer> findByCustomerIdAndUser_UserIdAndDeletedAtIsNull(Integer customerId, Integer userId);

    Optional<Customer> findByCustomerIdAndMerchant_MerchantIdAndDeletedAtIsNull(Integer customerId, Integer merchantId);

}
