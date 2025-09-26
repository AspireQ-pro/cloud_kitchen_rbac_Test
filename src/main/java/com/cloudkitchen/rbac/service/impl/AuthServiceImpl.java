package com.cloudkitchen.rbac.service.impl;

import com.cloudkitchen.rbac.domain.entity.*;
import com.cloudkitchen.rbac.dto.auth.*;
import com.cloudkitchen.rbac.enums.UserType;
import com.cloudkitchen.rbac.exception.AuthExceptions;
import com.cloudkitchen.rbac.repository.*;
import com.cloudkitchen.rbac.service.*;
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
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private final UserRepository users;
    private final MerchantRepository merchants;
    private final RoleRepository roles;
    private final UserRoleRepository userRoles;
    private final PasswordEncoder encoder;
    private final OtpService otpService;
    private final OtpAuditService otpAuditService;
    private final SmsService smsService;
    private final JwtTokenProvider jwt;
    private final UserSessionRepository sessionRepo;
    
    public AuthServiceImpl(UserRepository users, MerchantRepository merchants, RoleRepository roles,
            UserRoleRepository userRoles, PasswordEncoder encoder, OtpService otpService,
            OtpAuditService otpAuditService, SmsService smsService, JwtTokenProvider jwt,
            UserSessionRepository sessionRepo) {
        this.users = users;
        this.merchants = merchants;
        this.roles = roles;
        this.userRoles = userRoles;
        this.encoder = encoder;
        this.otpService = otpService;
        this.otpAuditService = otpAuditService;
        this.smsService = smsService;
        this.jwt = jwt;
        this.sessionRepo = sessionRepo;
    }

    @Override
    @Transactional
    public AuthResponse registerUser(RegisterRequest req) {
        Merchant merchant = merchants.findById(req.getMerchantId())
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));
        
        if (users.findByPhoneAndMerchant_MerchantId(req.getPhone(), req.getMerchantId()).isPresent()) {
            throw new AuthExceptions.UserAlreadyExistsException("Phone already registered");
        }
        
        User user = createUser(req, merchant);
        users.save(user);
        
        assignCustomerRole(user, merchant);
        return buildTokens(user, null);
    }
    
    private User createUser(RegisterRequest req, Merchant merchant) {
        User user = new User();
        user.setMerchant(merchant);
        user.setPhone(req.getPhone());
        user.setUsername(req.getPhone());
        user.setFirstName(req.getFirstName());
        user.setLastName(req.getLastName());
        user.setUserType(req.getUserType() != null ? req.getUserType() : "customer");
        user.setPasswordHash(encoder.encode(req.getPassword()));
        user.setAddress(req.getAddress());
        return user;
    }
    
    private void assignCustomerRole(User user, Merchant merchant) {
        Role role = roles.findByRoleName("customer")
                .orElseThrow(() -> new IllegalStateException("Customer role not found"));
        
        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        userRole.setMerchant(merchant);
        userRole.setAssignedAt(LocalDateTime.now());
        userRoles.save(userRole);
    }

    @Override
    public AuthResponse login(AuthRequest req) {
        User user;
        
        // Merchant login (merchantId = 0)
        if (req.getMerchantId() != null && Integer.valueOf(0).equals(req.getMerchantId())) {
            user = users.findByUsername(req.getPhone())
                .or(() -> users.findByEmailAndMerchantIsNull(req.getPhone()))
                .orElseThrow(() -> new AuthExceptions.UserNotFoundException("Invalid credentials"));
            
            if (!List.of(UserType.SUPER_ADMIN.getValue(), UserType.MERCHANT.getValue()).contains(user.getUserType())) {
                throw new AuthExceptions.AccessDeniedException("Access denied");
            }
        }
        // Customer login (requires specific merchantId > 0)
        else if (req.getMerchantId() != null && req.getMerchantId() > 0) {
            user = users.findByPhoneAndMerchant_MerchantId(req.getPhone(), req.getMerchantId())
                    .orElseThrow(() -> new AuthExceptions.UserNotFoundException("Invalid credentials"));
        }
        // Invalid: no merchantId or merchantId < 0
        else {
            throw new IllegalArgumentException("MerchantId required: use 0 for merchants, >0 for customers");
        }
        
        if (!encoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new AuthExceptions.InvalidPasswordException("Invalid credentials");
        }
        
        return buildTokens(user, req.getMerchantId());
    }

    @Override
    @Transactional
    public void requestOtp(OtpRequest req) {
        User user = findUserForOtp(req);
        String code = otpService.generateOtp();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);
        
        Integer merchantId = user.getMerchant() != null ? user.getMerchant().getMerchantId() : null;
        users.updateOtpByPhone(req.getPhone(), code, expiresAt, merchantId);
        if (!smsService.sendOtp(req.getPhone(), code)) {
            throw new RuntimeException("Failed to send OTP");
        }
        String maskedPhone = maskPhoneNumber(req.getPhone());
        String sanitizedPhone = sanitizeForLogging(maskedPhone);
        otpAuditService.logOtp(user.getMerchant() != null ? user.getMerchant().getMerchantId() : null, 
                sanitizedPhone, "****", expiresAt);
    }
    
    private User findUserForOtp(OtpRequest req) {
        if (req.getMerchantId() != null && Integer.valueOf(0).equals(req.getMerchantId())) {
            User user = users.findByUsername(req.getPhone())
                    .orElseThrow(() -> new AuthExceptions.UserNotFoundException("User not found"));
            
            if (!UserType.MERCHANT.getValue().equals(user.getUserType())) {
                throw new AuthExceptions.AccessDeniedException("Only merchants can use merchantId = 0");
            }
            return user;
        }
        return req.getMerchantId() != null 
            ? users.findByPhoneAndMerchant_MerchantId(req.getPhone(), req.getMerchantId())
                .orElseThrow(() -> new AuthExceptions.UserNotFoundException("User not found"))
            : users.findByUsername(req.getPhone())
                .orElseThrow(() -> new AuthExceptions.UserNotFoundException("User not found"));
    }

    @Override
    @Transactional
    public AuthResponse verifyOtp(OtpVerifyRequest req) {
        User user = findUserForOtp(new OtpRequest(req.getPhone(), req.getMerchantId()));
        
        if (!isOtpValid(req.getOtp(), user.getOtpCodeInternal()) || 
            user.getOtpExpiresAt() == null || 
            user.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }
        
        user.setOtpCode(null);
        user.setOtpExpiresAt(null);
        users.save(user);
        
        Integer merchantIdForToken = (req.getMerchantId() != null && Integer.valueOf(0).equals(req.getMerchantId())) 
                ? (user.getMerchant() != null ? user.getMerchant().getMerchantId() : null)
                : req.getMerchantId();
        
        return buildTokens(user, merchantIdForToken);
    }

    @Override
    public AuthResponse refresh(RefreshTokenRequest req) {
        try {
            if (jwt.isTokenBlacklisted(req.getRefreshToken())) {
                throw new AuthExceptions.InvalidPasswordException("Token has been revoked");
            }
            Claims claims = jwt.parse(req.getRefreshToken());
            Integer userId = Integer.valueOf(claims.getSubject());
            Integer merchantId = (Integer) claims.get("merchantId");
            User user = users.findById(userId).orElseThrow(() -> new AuthExceptions.UserNotFoundException("User not found"));
            
            // Blacklist the old refresh token for security (token rotation)
            jwt.blacklistToken(req.getRefreshToken());
            
            return buildTokens(user, merchantId);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            throw new AuthExceptions.InvalidPasswordException("Invalid refresh token", e);
        }
    }

    private AuthResponse buildTokens(User user, Integer merchantId) {
        Integer actualMerchantId = merchantId != null ? merchantId : 
                (user.getMerchant() != null ? user.getMerchant().getMerchantId() : null);
        List<String> roleNames = userRoles.findRoleNames(user.getUserId(), actualMerchantId);
        
        String accessToken = jwt.createAccessToken(user.getUserId(), actualMerchantId, roleNames, List.of());
        String refreshToken = jwt.createRefreshToken(user.getUserId(), actualMerchantId);
        
        createUserSession(user.getUserId(), accessToken);
        
        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(86400);
        response.setUserId(user.getUserId());
        response.setMerchantId(actualMerchantId);
        response.setPhone(user.getPhone());
        response.setRoles(roleNames);
        return response;
    }
    
    private void createUserSession(Integer userId, String accessToken) {
        UserSession session = new UserSession();
        session.setUserId(userId);
        session.setTokenHash(hashToken(accessToken));
        session.setCreatedAt(LocalDateTime.now());
        session.setExpiresAt(LocalDateTime.now().plusDays(1));
        sessionRepo.save(session);
    }
    
    private boolean isOtpValid(String inputOtp, String storedOtp) {
        if (inputOtp == null || storedOtp == null) return false;
        return inputOtp.equals(storedOtp);
    }
    
    private String hashToken(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("Token hashing failed", e);
        }
    }
    
    private String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return "****" + phone.substring(phone.length() - 4);
    }
    
    private String sanitizeForLogging(String input) {
        if (input == null) return null;
        return input.replaceAll("[\\r\\n\\t]", "_").replaceAll("[^\\w\\s*-]", "");
    }

    @Override
    @Transactional
    public void logout(Integer userId) {
        sessionRepo.deactivateUserSessions(userId);
    }
}