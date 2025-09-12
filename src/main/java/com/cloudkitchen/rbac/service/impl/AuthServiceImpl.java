package com.cloudkitchen.rbac.service.impl;

import com.cloudkitchen.rbac.domain.entity.*;
import com.cloudkitchen.rbac.dto.auth.*;
import com.cloudkitchen.rbac.repository.*;
import com.cloudkitchen.rbac.service.AuthService;
import com.cloudkitchen.rbac.service.OtpService;
import com.cloudkitchen.rbac.service.OtpAuditService;
import com.cloudkitchen.rbac.service.SmsService;

import com.cloudkitchen.rbac.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);
    private final UserRepository users;
    private final MerchantRepository merchants;
    private final RoleRepository roles;
    private final UserRoleRepository userRoles;
    private final OtpLogRepository otpLogs;
    private final PasswordEncoder encoder;
    private final OtpService otpService;
    private final OtpAuditService otpAuditService;
    private final SmsService smsService;
    private final JwtTokenProvider jwt;

    public AuthServiceImpl(UserRepository users, MerchantRepository merchants, RoleRepository roles,
            UserRoleRepository userRoles, OtpLogRepository otpLogs, PasswordEncoder encoder,
            OtpService otpService, OtpAuditService otpAuditService, SmsService smsService, JwtTokenProvider jwt) {
        this.users = users;
        this.merchants = merchants;
        this.roles = roles;
        this.userRoles = userRoles;
        this.otpLogs = otpLogs;
        this.encoder = encoder;
        this.otpService = otpService;
        this.otpAuditService = otpAuditService;
        this.smsService = smsService;
        this.jwt = jwt;
    }

    @Override
    @Transactional
    public AuthResponse registerCustomer(RegisterRequest req) {
        try {
            // Merchant is required for customer registration
            if (req.getMerchantId() == null) {
                throw new IllegalArgumentException("Merchant ID is required for customer registration");
            }
            
            Merchant merchant = merchants.findById(req.getMerchantId())
                    .orElseThrow(() -> new IllegalArgumentException("Merchant not found: " + req.getMerchantId()));
            
            logger.info("Merchant found: {}", merchant.getMerchantName());
            
            // Check for duplicate phone number within merchant
            if (users.findByPhoneAndMerchant_MerchantId(req.getPhone(), req.getMerchantId()).isPresent()) {
                throw new IllegalArgumentException("Phone number already registered with this merchant");
            }
            
            User user = new User();
            user.setMerchant(merchant);
            user.setPhone(req.getPhone());
            user.setEmail(req.getEmail());
            user.setUsername(req.getPhone());
            user.setFirstName(req.getFirstName());
            user.setLastName(req.getLastName());
            user.setAddress(req.getAddress());
            user.setCity(req.getCity());
            user.setState(req.getState());
            user.setPincode(req.getPincode());
            user.setUserType("customer");
            user.setPasswordHash(encoder.encode(req.getPassword()));
            user.setPreferredLoginMethod("password"); // Set to password for registration
            users.save(user);

            Role customerRole = roles.findByRoleName("customer")
                    .orElseThrow(() -> new IllegalStateException("Role 'customer' missing from database"));
            
            // Check if user-role assignment already exists
            if (!userRoles.existsByUserAndRoleAndMerchant(user, customerRole, merchant)) {
                UserRole userRole = new UserRole();
                userRole.setUser(user);
                userRole.setRole(customerRole);
                userRole.setMerchant(merchant);
                userRole.setAssignedAt(LocalDateTime.now());
                userRoles.save(userRole);
            }

            return buildTokens(user, null);
        } catch (Exception e) {
            logger.error("Registration error: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public AuthResponse loginWithPassword(AuthRequest req) {
        User user = loadByPhoneAndMerchant(req.getPhone(), null);
        if (user.getPasswordHash() == null || !encoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        return buildTokens(user, null);
    }

    @Override
    @Transactional
    public void requestOtp(OtpRequest req) {
        try {
            User u = users.findByUsername(req.getPhone()).orElse(null);
            
            if (u == null) {
                // Create new user
                Merchant m = null;
                if (req.getMerchantId() != null) {
                    m = merchants.findById(req.getMerchantId()).orElse(null);
                    if (m == null) {
                        logger.warn("Merchant {} not found, creating user without merchant", req.getMerchantId());
                    }
                }

                u = new User();
                u.setMerchant(m);
                u.setPhone(req.getPhone());
                u.setUsername(req.getPhone());
                u.setUserType("customer");
                u.setGuest(false);
                u.setPreferredLoginMethod("otp"); // Set to OTP for OTP-based creation
                u = users.save(u);
                logger.info("Created new user with ID: {} for phone: {}", u.getUserId(), req.getPhone());

                // Try to assign customer role if it exists
                Role customer = roles.findByRoleName("customer").orElse(null);
                if (customer != null) {
                    UserRole ur = new UserRole();
                    ur.setUser(u);
                    ur.setRole(customer);
                    ur.setMerchant(m);
                    ur.setAssignedAt(LocalDateTime.now());
                    userRoles.save(ur);
                    logger.info("Assigned customer role to user: {}", u.getUserId());
                } else {
                    logger.warn("Customer role not found, user created without role");
                }
            }

            String code = otpService.generateOtp();
            logger.info("Generated OTP for phone: {}, Length: {}", req.getPhone(), code.length());
            
            u.setOtpCode(code);
            u.setOtpExpiresAt(LocalDateTime.now().plusMinutes(5));
            u.setOtpAttempts(0);
            
            users.save(u);
            logger.info("OTP stored successfully for user ID: {}, phone: {}", u.getUserId(), u.getPhone());
            
            // Send OTP via SMS service
            smsService.sendOtp(u.getPhone(), code);
            
            // Audit logging
            Integer merchantId = u.getMerchant() != null ? u.getMerchant().getMerchantId() : null;
            otpAuditService.logOtp(merchantId, u.getPhone(), code, u.getOtpExpiresAt());
            
        } catch (Exception e) {
            logger.error("Failed to process OTP request for phone: {}", req.getPhone(), e);
            throw new RuntimeException("OTP request failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public AuthResponse verifyOtp(OtpVerifyRequest req) {
        User u = loadByPhoneAndMerchant(req.getPhone(), null);
        if (u.getOtpExpiresAt() == null || u.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("OTP expired");
        }
        int currentAttempts = u.getOtpAttempts() != null ? u.getOtpAttempts() : 0;
        if (currentAttempts >= 5) {
            throw new IllegalArgumentException("Too many attempts");
        }
        if (!req.getOtp().equals(u.getOtpCode())) {
            int newAttempts = currentAttempts + 1;
            u.setOtpAttempts(newAttempts);
            users.save(u);
            
            // Log failed attempt
            otpAuditService.updateOtpFailed(u.getPhone(), newAttempts);
            
            throw new IllegalArgumentException("Invalid OTP");
        }

        // Log successful verification
        otpAuditService.updateOtpVerified(u.getPhone(), req.getOtp());
        
        u.setOtpCode(null);
        u.setOtpExpiresAt(null);
        u.setOtpAttempts(0);
        users.save(u);

        return buildTokens(u, null);
    }

    @Override
    public AuthResponse refresh(RefreshTokenRequest req) {
        Claims claims = jwt.parse(req.getRefreshToken());
        Integer userId = Integer.valueOf(claims.getSubject());
        Integer merchantId = (Integer) claims.get("merchantId");
        User u = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User missing"));
        return buildTokens(u, merchantId);
    }
    
    @Override
    public AuthResponse merchantLogin(MerchantLoginRequest req) {
        // Find merchant by email
        Merchant merchant = merchants.findByEmail(req.getEmail())
            .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        
        if (!merchant.isActive()) {
            throw new IllegalArgumentException("Merchant account is inactive");
        }
        
        // Find merchant user
        User merchantUser = users.findByMerchantAndUserType(merchant, "merchant")
            .orElseThrow(() -> new IllegalArgumentException("Merchant user not found"));
        
        if (merchantUser.getPasswordHash() == null || !encoder.matches(req.getPassword(), merchantUser.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        
        return buildTokens(merchantUser, merchant.getMerchantId());
    }

    private User loadByPhoneAndMerchant(String phone, Integer merchantId) {
        return users.findByUsername(phone)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private AuthResponse buildTokens(User u, Integer merchantId) {
        // Use user's actual merchant ID if not provided
        Integer actualMerchantId = merchantId != null ? merchantId : 
                (u.getMerchant() != null ? u.getMerchant().getMerchantId() : null);
        
        List<String> roleNames = userRoles.findRoleNames(u.getUserId(), actualMerchantId);
        List<String> permissions = List.of(); // Empty permissions for now
        String access = jwt.createAccessToken(u.getUserId(), actualMerchantId, roleNames, permissions);
        String refresh = jwt.createRefreshToken(u.getUserId(), actualMerchantId);
        
        AuthResponse res = new AuthResponse();
        res.setAccessToken(access);
        res.setRefreshToken(refresh);
        res.setExpiresIn(86400);
        res.setUserId(u.getUserId());
        res.setMerchantId(actualMerchantId);
        res.setRoles(roleNames);
        return res;
    }
}
