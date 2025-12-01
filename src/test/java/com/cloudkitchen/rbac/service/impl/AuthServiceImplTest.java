package com.cloudkitchen.rbac.service.impl;

import com.cloudkitchen.rbac.domain.entity.Merchant;
import com.cloudkitchen.rbac.domain.entity.User;
import com.cloudkitchen.rbac.dto.auth.AuthRequest;
import com.cloudkitchen.rbac.dto.auth.AuthResponse;
import com.cloudkitchen.rbac.dto.auth.RegisterRequest;
import com.cloudkitchen.rbac.exception.BusinessExceptions.InvalidCredentialsException;
import com.cloudkitchen.rbac.exception.BusinessExceptions.UserAlreadyExistsException;
import com.cloudkitchen.rbac.exception.BusinessExceptions.UserNotFoundException;
import com.cloudkitchen.rbac.repository.MerchantRepository;
import com.cloudkitchen.rbac.repository.RoleRepository;
import com.cloudkitchen.rbac.repository.UserRepository;
import com.cloudkitchen.rbac.repository.UserRoleRepository;
import com.cloudkitchen.rbac.security.JwtTokenProvider;
import com.cloudkitchen.rbac.service.OtpAuditService;
import com.cloudkitchen.rbac.service.OtpService;
import com.cloudkitchen.rbac.service.SmsService;
import com.cloudkitchen.rbac.service.ValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private OtpService otpService;

    @Mock
    private OtpAuditService otpAuditService;

    @Mock
    private SmsService smsService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private ValidationService validationService;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private AuthRequest authRequest;
    private User user;
    private Merchant merchant;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");
        registerRequest.setPhone("1234567890");
        registerRequest.setEmail("john@example.com");
        registerRequest.setPassword("Password123!");
        registerRequest.setMerchantId(1);

        authRequest = new AuthRequest();
        authRequest.setUsername("1234567890");
        authRequest.setPassword("Password123!");
        authRequest.setMerchantId(1);

        merchant = new Merchant();
        merchant.setMerchantId(1);
        merchant.setMerchantName("Test Merchant");

        user = new User();
        user.setUserId(1);
        user.setUsername("1234567890");
        user.setPhone("1234567890");
        user.setEmail("john@example.com");
        user.setPasswordHash("encodedPassword");
        user.setUserType("customer");
        user.setMerchant(merchant);
        user.setActive(true);
    }

    @Test
    void testRegisterUser_Success() {
        // Given
        when(merchantRepository.findById(1)).thenReturn(Optional.of(merchant));
        when(userRepository.existsByPhoneAndMerchant_MerchantId(anyString(), anyInt())).thenReturn(false);
        when(userRepository.existsByEmailAndMerchant_MerchantId(anyString(), anyInt())).thenReturn(false);
        when(userRepository.existsByUsernameAndMerchant_MerchantId(anyString(), anyInt())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtTokenProvider.createAccessToken(anyInt(), anyInt(), anyList(), anyList())).thenReturn("accessToken");
        when(jwtTokenProvider.createRefreshToken(anyInt(), anyInt())).thenReturn("refreshToken");

        // When
        AuthResponse response = authService.registerUser(registerRequest);

        // Then
        assertNotNull(response);
        assertEquals("accessToken", response.getAccessToken());
        assertEquals("refreshToken", response.getRefreshToken());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterUser_PhoneAlreadyExists() {
        // Given
        when(merchantRepository.findById(1)).thenReturn(Optional.of(merchant));
        when(userRepository.existsByPhoneAndMerchant_MerchantId(anyString(), anyInt())).thenReturn(true);

        // When & Then
        assertThrows(UserAlreadyExistsException.class, () -> {
            authService.registerUser(registerRequest);
        });
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLogin_Success() {
        // Given
        when(userRepository.findByUsernameAndMerchant_MerchantId(anyString(), anyInt()))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(anyInt(), anyInt(), anyList(), anyList())).thenReturn("accessToken");
        when(jwtTokenProvider.createRefreshToken(anyInt(), anyInt())).thenReturn("refreshToken");

        // When
        AuthResponse response = authService.login(authRequest);

        // Then
        assertNotNull(response);
        assertEquals("accessToken", response.getAccessToken());
        assertEquals("refreshToken", response.getRefreshToken());
    }

    @Test
    void testLogin_InvalidCredentials() {
        // Given
        when(userRepository.findByUsernameAndMerchant_MerchantId(anyString(), anyInt()))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // When & Then
        assertThrows(InvalidCredentialsException.class, () -> {
            authService.login(authRequest);
        });
    }

    @Test
    void testLogin_UserNotFound() {
        // Given
        when(userRepository.findByUsernameAndMerchant_MerchantId(anyString(), anyInt()))
                .thenReturn(Optional.empty());

        // When & Then
        assertThrows(UserNotFoundException.class, () -> {
            authService.login(authRequest);
        });
    }
}

