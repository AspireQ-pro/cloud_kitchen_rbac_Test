package com.cloudkitchen.rbac.service.impl;

import com.cloudkitchen.rbac.domain.entity.Merchant;
import com.cloudkitchen.rbac.domain.entity.User;
import com.cloudkitchen.rbac.dto.common.PageRequest;
import com.cloudkitchen.rbac.dto.common.PageResponse;
import com.cloudkitchen.rbac.dto.merchant.MerchantRequest;
import com.cloudkitchen.rbac.dto.merchant.MerchantResponse;
import com.cloudkitchen.rbac.exception.BusinessExceptions.MerchantAlreadyExistsException;
import com.cloudkitchen.rbac.exception.BusinessExceptions.MerchantNotFoundException;
import com.cloudkitchen.rbac.exception.BusinessExceptions.UserAlreadyExistsException;
import com.cloudkitchen.rbac.repository.MerchantRepository;
import com.cloudkitchen.rbac.repository.UserRepository;
import com.cloudkitchen.rbac.service.CloudStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MerchantServiceImplTest {

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CloudStorageService cloudStorageService;

    @InjectMocks
    private MerchantServiceImpl merchantService;

    private MerchantRequest merchantRequest;
    private Merchant merchant;
    private User user;

    @BeforeEach
    void setUp() {
        merchantRequest = new MerchantRequest();
        merchantRequest.setMerchantName("Test Merchant");
        merchantRequest.setPhone("1234567890");
        merchantRequest.setEmail("test@example.com");
        merchantRequest.setUsername("testuser");
        merchantRequest.setPassword("Password123!");

        merchant = new Merchant();
        merchant.setMerchantId(1);
        merchant.setMerchantName("Test Merchant");
        merchant.setPhone("1234567890");
        merchant.setEmail("test@example.com");
        merchant.setActive(true);

        user = new User();
        user.setUserId(1);
        user.setUsername("testuser");
        user.setUserType("merchant");
        user.setMerchant(merchant);
    }

    @Test
    void testCreateMerchant_Success() {
        // Given
        when(merchantRepository.existsByPhone(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(merchantRepository.save(any(Merchant.class))).thenReturn(merchant);
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");

        // When
        MerchantResponse response = merchantService.createMerchant(merchantRequest);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getId());
        assertEquals("Test Merchant", response.getMerchantName());
        verify(merchantRepository, times(1)).save(any(Merchant.class));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testCreateMerchant_PhoneAlreadyExists() {
        // Given
        when(merchantRepository.existsByPhone(anyString())).thenReturn(true);

        // When & Then
        assertThrows(MerchantAlreadyExistsException.class, () -> {
            merchantService.createMerchant(merchantRequest);
        });
        verify(merchantRepository, never()).save(any(Merchant.class));
    }

    @Test
    void testCreateMerchant_UsernameAlreadyExists() {
        // Given
        when(merchantRepository.existsByPhone(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        // When & Then
        assertThrows(UserAlreadyExistsException.class, () -> {
            merchantService.createMerchant(merchantRequest);
        });
        verify(merchantRepository, never()).save(any(Merchant.class));
    }

    @Test
    void testGetMerchantById_Success() {
        // Given
        when(merchantRepository.findById(1)).thenReturn(Optional.of(merchant));
        when(userRepository.findByMerchantAndUserType(any(), anyString())).thenReturn(Optional.of(user));

        // When
        MerchantResponse response = merchantService.getMerchantById(1);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getId());
        assertEquals("Test Merchant", response.getMerchantName());
    }

    @Test
    void testGetMerchantById_NotFound() {
        // Given
        when(merchantRepository.findById(1)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(MerchantNotFoundException.class, () -> {
            merchantService.getMerchantById(1);
        });
    }

    @Test
    void testGetAllMerchants_WithPagination() {
        // Given
        PageRequest pageRequest = new PageRequest(0, 10);
        List<Merchant> merchants = Arrays.asList(merchant);
        Page<Merchant> merchantPage = new PageImpl<>(merchants, org.springframework.data.domain.PageRequest.of(0, 10), 1);
        
        when(merchantRepository.findAll(any(Pageable.class))).thenReturn(merchantPage);
        when(userRepository.findByMerchantAndUserType(any(), anyString())).thenReturn(Optional.of(user));

        // When
        PageResponse<MerchantResponse> response = merchantService.getAllMerchants(pageRequest);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getContent().size());
        assertEquals(0, response.getPage());
        assertEquals(10, response.getSize());
    }

    @Test
    void testGetAllMerchants_WithFiltering() {
        // Given
        PageRequest pageRequest = new PageRequest(0, 10);
        List<Merchant> merchants = Arrays.asList(merchant);
        Page<Merchant> merchantPage = new PageImpl<>(merchants, org.springframework.data.domain.PageRequest.of(0, 10), 1);
        
        when(merchantRepository.findByActive(eq(true), any(Pageable.class))).thenReturn(merchantPage);
        when(userRepository.findByMerchantAndUserType(any(), anyString())).thenReturn(Optional.of(user));

        // When
        PageResponse<MerchantResponse> response = merchantService.getAllMerchants(pageRequest, "active", null);

        // Then
        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        verify(merchantRepository, times(1)).findByActive(eq(true), any(Pageable.class));
    }

    @Test
    void testUpdateMerchant_Success() {
        // Given
        MerchantRequest updateRequest = new MerchantRequest();
        updateRequest.setMerchantName("Updated Merchant");
        updateRequest.setEmail("updated@example.com");

        when(merchantRepository.findById(1)).thenReturn(Optional.of(merchant));
        when(merchantRepository.save(any(Merchant.class))).thenReturn(merchant);
        when(userRepository.findByMerchantAndUserType(any(), anyString())).thenReturn(Optional.of(user));

        // When
        MerchantResponse response = merchantService.updateMerchant(1, updateRequest);

        // Then
        assertNotNull(response);
        verify(merchantRepository, times(1)).save(any(Merchant.class));
    }

    @Test
    void testDeleteMerchant_Success() {
        // Given
        when(merchantRepository.findById(1)).thenReturn(Optional.of(merchant));
        doNothing().when(userRepository).deleteByMerchant(any(Merchant.class));
        doNothing().when(merchantRepository).deleteById(1);

        // When
        merchantService.deleteMerchant(1);

        // Then
        verify(userRepository, times(1)).deleteByMerchant(any(Merchant.class));
        verify(merchantRepository, times(1)).deleteById(1);
    }
}

