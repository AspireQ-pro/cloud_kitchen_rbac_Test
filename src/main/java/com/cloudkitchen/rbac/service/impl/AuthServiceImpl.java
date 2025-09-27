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
    private final OtpLogRepository otpLogRepository;
    private final SmsService smsService;
    private final JwtTokenProvider jwt;
    private final UserSessionRepository sessionRepo;
    
    public AuthServiceImpl(UserRepository users, MerchantRepository merchants, RoleRepository roles,
            UserRoleRepository userRoles, PasswordEncoder encoder, OtpService otpService,
            OtpAuditService otpAuditService, SmsService smsService, JwtTokenProvider jwt,
            UserSessionRepository sessionRepo, OtpLogRepository otpLogRepository) {
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
        this.otpLogRepository = otpLogRepository;
    }

    @Override
    @Transactional
    public AuthResponse registerUser(RegisterRequest req) {
        // Auto-determine user type based on merchantId
        String userType = determineUserType(req.getMerchantId());
        
        // Additional validation
        validateRegistrationRequest(req);
        
        // Handle different user types
        Merchant merchant = null;
        if (!"super_admin".equals(userType)) {
            merchant = merchants.findById(req.getMerchantId())
                    .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));
            
            // Check for duplicate phone number within merchant
            if (users.findByPhoneAndMerchant_MerchantId(req.getPhone(), req.getMerchantId()).isPresent()) {
                throw new AuthExceptions.UserAlreadyExistsException("Phone number already registered");
            }
        } else {
            // For super_admin, check global uniqueness
            if (users.findByPhoneAndMerchantIsNull(req.getPhone()).isPresent()) {
                throw new AuthExceptions.UserAlreadyExistsException("Phone number already registered");
            }
        }
        
        User user = createUser(req, merchant, userType);
        users.save(user);
        
        assignUserRole(user, userType, merchant);
        return buildTokens(user, merchant != null ? merchant.getMerchantId() : null);
    }
    
    private void validateRegistrationRequest(RegisterRequest req) {
        // Validate phone format
        if (req.getPhone() == null || !req.getPhone().matches("^[6-9]\\d{9}$")) {
            throw new IllegalArgumentException("Phone must be a valid 10-digit Indian mobile number starting with 6-9");
        }
        
        // Validate password strength
        if (req.getPassword() == null || !req.getPassword().matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,128}$")) {
            throw new IllegalArgumentException("Password must be 8-128 characters with at least one uppercase letter, one lowercase letter, one digit, and one special character (@$!%*?&)");
        }
        
        // Validate names
        if (req.getFirstName() == null || req.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required and cannot be empty");
        }
        if (req.getLastName() == null || req.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required and cannot be empty");
        }
        
        // Validate name format
        if (!req.getFirstName().matches("^[a-zA-Z\\s\\-']{2,50}$")) {
            throw new IllegalArgumentException("First name must be 2-50 characters and contain only letters, spaces, hyphens, and apostrophes");
        }
        if (!req.getLastName().matches("^[a-zA-Z\\s\\-']{2,50}$")) {
            throw new IllegalArgumentException("Last name must be 2-50 characters and contain only letters, spaces, hyphens, and apostrophes");
        }
    }
    
    private String determineUserType(Integer merchantId) {
        if (merchantId == null || merchantId == 0) {
            return "super_admin";
        } else {
            return "customer"; // Default to customer for merchantId > 0
        }
    }
    
    private User createUser(RegisterRequest req, Merchant merchant, String userType) {
        User user = new User();
        user.setMerchant(merchant);
        user.setPhone(req.getPhone());
        user.setUsername(req.getPhone());
        user.setFirstName(req.getFirstName());
        user.setLastName(req.getLastName());
        user.setUserType(userType);
        user.setPasswordHash(encoder.encode(req.getPassword()));
        user.setAddress(req.getAddress());
        return user;
    }
    
    private void assignUserRole(User user, String userType, Merchant merchant) {
        String roleName = switch (userType) {
            case "super_admin" -> "super_admin";
            case "merchant" -> "merchant_admin";
            case "customer" -> "customer";
            default -> "customer";
        };
        
        Role role = roles.findByRoleName(roleName)
                .orElseThrow(() -> new IllegalStateException(roleName + " role not found"));
        
        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        userRole.setMerchant(merchant); // null for super_admin
        userRole.setAssignedAt(LocalDateTime.now());
        userRoles.save(userRole);
    }

    @Override
    public AuthResponse login(AuthRequest req) {
        User user;
        
        // Merchant login (merchantId = 0)
        if (req.getMerchantId() != null && Integer.valueOf(0).equals(req.getMerchantId())) {
            user = users.findByPhoneAndMerchantIsNull(req.getPhone())
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


    
    private User findUserForOtp(OtpRequest req) {
        // For merchantId = 0: Find merchant/super_admin users (no merchant association)
        if (req.getMerchantId() != null && Integer.valueOf(0).equals(req.getMerchantId())) {
            return users.findByPhoneAndMerchantIsNull(req.getPhone())
                    .orElseThrow(() -> new AuthExceptions.UserNotFoundException("User not found"));
        }
        // For merchantId > 0: Find customer users associated with that merchant
        else if (req.getMerchantId() != null && req.getMerchantId() > 0) {
            return users.findByPhoneAndMerchant_MerchantId(req.getPhone(), req.getMerchantId())
                    .orElseThrow(() -> new AuthExceptions.UserNotFoundException("User not found"));
        }
        // For null merchantId: Find any user by phone (fallback)
        else {
            return users.findByPhone(req.getPhone())
                    .orElseThrow(() -> new AuthExceptions.UserNotFoundException("User not found"));
        }
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
    

    
    private int getCurrentOtpAttempts(String phone) {
        Integer attempts = otpLogRepository.getCurrentAttemptCount(phone);
        return attempts != null ? attempts : 0;
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
    
    @Override
    @Transactional
    public void requestOtp(OtpRequest req) {
        log.info("OTP request received for phone: {}, merchantId: {}", maskPhoneNumber(req.getPhone()), req.getMerchantId());
        
        try {
            // Find user
            User user = findUserForOtp(req);
            log.info("User found: {}, userType: {}", user.getUserId(), user.getUserType());
            
            // Generate OTP
            String code = otpService.generateOtp();
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);
            log.info("OTP generated: ****, expires at: {}", expiresAt);
            
            // Update user with OTP
            user.setOtpCode(code);
            user.setOtpExpiresAt(expiresAt);
            user.setOtpAttempts(0);
            users.save(user);
            log.info("User updated with OTP");
            
            // Send SMS
            boolean smsSent = smsService.sendOtp(req.getPhone(), code);
            log.info("SMS sent: {}", smsSent);
            
            if (!smsSent) {
                throw new RuntimeException("Failed to send OTP");
            }
            
            log.info("OTP request completed successfully");
            
        } catch (Exception e) {
            log.error("OTP request failed for phone: {}, error: {}", maskPhoneNumber(req.getPhone()), e.getMessage(), e);
            throw e;
        }
    }
    

    
    @Override
    @Transactional
    public boolean verifyOtp(OtpVerifyRequest req) {
        User user = findUserForOtp(new OtpRequest(req.getPhone(), req.getMerchantId()));
        
        // Check if OTP is valid
        if (!isOtpValid(req.getOtp(), user.getOtpCode()) || 
            user.getOtpExpiresAt() == null || 
            user.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            
            int currentAttempts = getCurrentOtpAttempts(req.getPhone());
            otpAuditService.updateOtpFailed(req.getPhone(), currentAttempts + 1);
            return false;
        }
        
        // OTP is valid - update audit log
        otpAuditService.updateOtpVerified(req.getPhone(), "****");
        
        // Clear OTP from user record
        user.setOtpCode(null);
        user.setOtpExpiresAt(null);
        users.save(user);
        
        return true;
    }
    
    @Override
    @Transactional
    public void resetPassword(PasswordResetRequest req) {
        User user = findUserForOtp(new OtpRequest(req.getPhone(), req.getMerchantId()));
        
        // Create OtpVerifyRequest object
        OtpVerifyRequest otpVerifyReq = new OtpVerifyRequest();
        otpVerifyReq.setPhone(req.getPhone());
        otpVerifyReq.setMerchantId(req.getMerchantId());
        otpVerifyReq.setOtp(req.getOtp());
        
        // Verify OTP using the generic verify method
        if (!verifyOtp(otpVerifyReq)) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }
        
        // Validate password strength before encoding
        if (!req.getNewPassword().matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$")) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character");
        }
        
        // Update password (OTP already verified and cleared by verifyOtp)
        user.setPasswordHash(encoder.encode(req.getNewPassword()));
        users.save(user);
    }
}