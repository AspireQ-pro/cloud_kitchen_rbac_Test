package com.cloudkitchen.rbac.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cloudkitchen.rbac.domain.entity.Merchant;
import com.cloudkitchen.rbac.domain.entity.Role;
import com.cloudkitchen.rbac.domain.entity.User;
import com.cloudkitchen.rbac.domain.entity.UserRole;
import com.cloudkitchen.rbac.domain.entity.UserSession;
import com.cloudkitchen.rbac.dto.auth.AuthRequest;
import com.cloudkitchen.rbac.dto.auth.AuthResponse;
import com.cloudkitchen.rbac.dto.auth.OtpRequest;
import com.cloudkitchen.rbac.dto.auth.OtpVerifyRequest;
import com.cloudkitchen.rbac.dto.auth.PasswordResetRequest;
import com.cloudkitchen.rbac.dto.auth.RefreshTokenRequest;
import com.cloudkitchen.rbac.dto.auth.RegisterRequest;
import com.cloudkitchen.rbac.enums.UserType;
import com.cloudkitchen.rbac.repository.MerchantRepository;
import com.cloudkitchen.rbac.repository.OtpLogRepository;
import com.cloudkitchen.rbac.repository.RoleRepository;
import com.cloudkitchen.rbac.repository.UserRepository;
import com.cloudkitchen.rbac.repository.UserRoleRepository;
import com.cloudkitchen.rbac.repository.UserSessionRepository;
import com.cloudkitchen.rbac.security.JwtTokenProvider;
import com.cloudkitchen.rbac.service.AuthService;
import com.cloudkitchen.rbac.service.OtpAuditService;
import com.cloudkitchen.rbac.service.OtpService;
import com.cloudkitchen.rbac.service.SmsService;

import io.jsonwebtoken.Claims;

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
        String userType = determineUserType(req.getMerchantId());
        validateRegistrationRequest(req);
        
        Merchant merchant = null;
        if (!"super_admin".equals(userType)) {
            merchant = merchants.findById(req.getMerchantId())
                    .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));
            
            if (users.findByPhoneAndMerchant_MerchantId(req.getPhone(), req.getMerchantId()).isPresent()) {
                throw new RuntimeException("Phone number " + maskPhoneNumber(req.getPhone()) + " is already registered for this merchant. Please use a different phone number or login instead.");
            }
        } else {
            if (users.findByPhoneAndMerchantIsNull(req.getPhone()).isPresent()) {
                throw new RuntimeException("Phone number " + maskPhoneNumber(req.getPhone()) + " is already registered as admin/merchant. Please use a different phone number or login instead.");
            }
        }
        
        User user = createUser(req, merchant, userType);
        users.save(user);
        
        assignUserRole(user, userType, merchant);
        return buildTokens(user, merchant != null ? merchant.getMerchantId() : null);
    }
    
    private void validateRegistrationRequest(RegisterRequest req) {
        if (req.getPhone() == null || !req.getPhone().matches("^[6-9]\\d{9}$")) {
            throw new IllegalArgumentException("Phone must be a valid 10-digit Indian mobile number starting with 6-9");
        }
        
        if (req.getPassword() == null || !req.getPassword().matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,128}$")) {
            throw new IllegalArgumentException("Password must be 8-128 characters with at least one uppercase letter, one lowercase letter, one digit, and one special character (@$!%*?&)");
        }
        
        if (req.getFirstName() == null || req.getFirstName().trim().isEmpty()) {
            throw new IllegalArgumentException("First name is required and cannot be empty");
        }
        if (req.getLastName() == null || req.getLastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Last name is required and cannot be empty");
        }
        
        if (!req.getFirstName().matches("^[a-zA-Z\\s\\-']{2,50}$")) {
            throw new IllegalArgumentException("First name must be 2-50 characters and contain only letters, spaces, hyphens, and apostrophes");
        }
        if (!req.getLastName().matches("^[a-zA-Z\\s\\-']{2,50}$")) {
            throw new IllegalArgumentException("Last name must be 2-50 characters and contain only letters, spaces, hyphens, and apostrophes");
        }
    }
    
    private String determineUserType(Integer merchantId) {
        if (merchantId == null) {
            return "super_admin";
        } else if (merchantId == 0) {
            return "merchant";
        } else {
            return "customer";
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
        String roleName;
        if ("super_admin".equals(userType)) {
            roleName = "super_admin";
        } else if ("merchant".equals(userType)) {
            roleName = "merchant_admin";
        } else if ("customer".equals(userType)) {
            roleName = "customer";
        } else {
            roleName = "customer";
        }
        
        log.debug("User role assignment: userType={}, roleName={}", userType, roleName);
        
        Role role = roles.findByRoleName(roleName)
                .orElseThrow(() -> new IllegalStateException(roleName + " role not found"));
        
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
        
        if (req.getMerchantId() != null && Integer.valueOf(0).equals(req.getMerchantId())) {
            user = users.findByPhoneAndMerchantIsNull(req.getPhone())
                .or(() -> users.findByEmailAndMerchantIsNull(req.getPhone()))
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));
            
            if (!List.of(UserType.SUPER_ADMIN.getValue(), UserType.MERCHANT.getValue()).contains(user.getUserType())) {
                throw new RuntimeException("Access denied");
            }
        }
        else if (req.getMerchantId() != null && req.getMerchantId() > 0) {
            user = users.findByPhoneAndMerchant_MerchantId(req.getPhone(), req.getMerchantId())
                    .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        }
        else {
            throw new IllegalArgumentException("MerchantId required: use 0 for merchants, >0 for customers");
        }
        
        if (!encoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }
        
        return buildTokens(user, req.getMerchantId());
    }
    
    private User findUserForOtp(OtpRequest req) {
        if (req.getMerchantId() != null && Integer.valueOf(0).equals(req.getMerchantId())) {
            return users.findByPhoneAndMerchantIsNull(req.getPhone())
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }
        else if (req.getMerchantId() != null && req.getMerchantId() > 0) {
            return users.findByPhoneAndMerchant_MerchantId(req.getPhone(), req.getMerchantId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }
        else {
            return users.findByPhone(req.getPhone())
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }
    }

    @Override
    public AuthResponse refresh(RefreshTokenRequest req) {
        try {
            if (jwt.isTokenBlacklisted(req.getRefreshToken())) {
                throw new RuntimeException("Token has been revoked");
            }
            Claims claims = jwt.parse(req.getRefreshToken());
            Integer userId = Integer.valueOf(claims.getSubject());
            Integer merchantId = (Integer) claims.get("merchantId");
            User user = users.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
            
            jwt.blacklistToken(req.getRefreshToken());
            
            return buildTokens(user, merchantId);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            throw new RuntimeException("Invalid refresh token: " + sanitizeForLogging(e.getMessage()), e);
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
    
    private void validateOtpRateLimit(String phone, String otpType) {
        LocalDateTime timeLimit = getTimeLimitByType(otpType);
        int maxRequests = getMaxRequestsByType(otpType);
        int recentRequests = otpLogRepository.countByPhoneAndCreatedOnAfter(phone, timeLimit);
        
        if (recentRequests >= maxRequests) {
            log.warn("Rate limit exceeded for phone: {}, type: {}, requests: {}", maskPhoneNumber(phone), otpType, recentRequests);
            throw new RuntimeException(String.format("Too many %s OTP requests. Please try again later.", otpType));
        }
    }
    
    private void invalidateExistingOtp(User user, String otpType) {
        if (user.getOtpCode() != null) {
            otpAuditService.updateOtpCancelled(user.getPhone(), "new_" + otpType + "_request");
            clearOtpData(user);
        }
    }
    
    private String generateOtpByType(String otpType) {
        // Generate 4-digit OTP for all types
        return otpService.generateOtp();
    }
    
    private LocalDateTime getExpiryByType(String otpType) {
        // Different expiry times based on OTP type
        switch (otpType) {
            case "password_reset":
                return LocalDateTime.now().plusMinutes(5); 
            case "phone_verification":
                return LocalDateTime.now().plusMinutes(5); 
            case "account_verification":
                return LocalDateTime.now().plusMinutes(5); 
            default:
                return LocalDateTime.now().plusMinutes(5);
        }
    }
    
    private LocalDateTime getTimeLimitByType(String otpType) {
        // Different rate limit windows based on OTP type
        switch (otpType) {
            case "password_reset":
            case "account_verification":
                return LocalDateTime.now().minusHours(1); // 1 hour window
            case "login":
                return LocalDateTime.now().minusMinutes(15); // 15 min window
            case "registration":
            case "phone_verification":
                return LocalDateTime.now().minusMinutes(30); // 30 min window
            default:
                return LocalDateTime.now().minusHours(1);
        }
    }
    
    private int getMaxRequestsByType(String otpType) {
        // Different rate limits based on OTP type
        switch (otpType) {
            case "password_reset":
            case "account_verification":
                return 3; // Max 3 per hour
            case "phone_verification":
                return 3; // Max 3 per 30 min
            default:
                return 3;
        }
    }
    
    private boolean sendOtpByType(String phone, String otpCode, String otpType) {
        // Send OTP with type-specific message
        return smsService.sendOtp(phone, otpCode); // SMS service handles the message
    }
    

    
    private void clearOtpData(User user) {
        user.setOtpCode(null);
        user.setOtpExpiresAt(null);
        user.setOtpAttempts(0);
        users.save(user);
    }
    
    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@$!%*?&";
        java.util.Random random = new java.util.Random();
        StringBuilder password = new StringBuilder(10);
        
        // Ensure at least one of each required type
        password.append(chars.charAt(random.nextInt(26))); // uppercase
        password.append(chars.charAt(26 + random.nextInt(26))); // lowercase
        password.append(chars.charAt(52 + random.nextInt(10))); // digit
        password.append(chars.charAt(62 + random.nextInt(7))); // special
        
        // Fill remaining 6 characters randomly
        for (int i = 4; i < 10; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        // Shuffle the password
        char[] array = password.toString().toCharArray();
        for (int i = array.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
        
        return new String(array);
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
        String maskedPhone = maskPhoneNumber(req.getPhone());
        String otpType = req.getOtpType() != null ? req.getOtpType() : "login";
        log.info("OTP request for phone: {}, merchantId: {}, type: {}", maskedPhone, req.getMerchantId(), otpType);
        
        try {
            // 1. Validate user exists
            User user = findUserForOtp(req);
            
            // 2. Check rate limiting based on OTP type
            validateOtpRateLimit(req.getPhone(), otpType);
            
            // 3. Invalidate any existing OTP
            invalidateExistingOtp(user, otpType);
            
            // 4. Generate secure OTP with type-specific settings
            String otpCode = generateOtpByType(otpType);
            LocalDateTime expiresAt = getExpiryByType(otpType);
            
            // 5. Store OTP securely
            user.setOtpCode(otpCode);
            user.setOtpExpiresAt(expiresAt);
            user.setOtpAttempts(0);
            users.save(user);
            
            // 6. Send OTP via SMS with type-specific message
            boolean smsSent = sendOtpByType(req.getPhone(), otpCode, otpType);
            String status = smsSent ? "sent" : "send_failed";
            
            // 7. Audit log with actual OTP type
            Integer merchantId = user.getMerchant() != null ? user.getMerchant().getMerchantId() : req.getMerchantId();
            otpAuditService.logOtp(merchantId, req.getPhone(), otpCode, otpType, status, expiresAt);
            
            if (!smsSent) {
                throw new RuntimeException("SMS service temporarily unavailable. Please try again.");
            }
            
            log.info("OTP sent successfully to phone: {}, type: {}", maskedPhone, otpType);
            
        } catch (Exception e) {
            log.error("OTP request failed for phone: {}, type: {}, error: {}", maskedPhone, otpType, sanitizeForLogging(e.getMessage()), e);
            throw e;
        }
    }
    
    @Override
    @Transactional
    public AuthResponse verifyOtp(OtpVerifyRequest req) {
        String maskedPhone = maskPhoneNumber(req.getPhone());
        log.info("OTP verification attempt for phone: {}, purpose: {}", maskedPhone, req.getPurpose());
        
        try {
            User user = findUserForOtp(new OtpRequest(req.getPhone(), req.getMerchantId()));
            
            // 1. Check if OTP exists
            if (user.getOtpCode() == null) {
                log.warn("No active OTP found for phone: {}", maskedPhone);
                return null;
            }
            
            // 2. Check expiry
            if (user.getOtpExpiresAt() == null || user.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
                otpAuditService.updateOtpExpired(req.getPhone());
                clearOtpData(user);
                log.warn("Expired OTP verification attempt for phone: {}", maskedPhone);
                return null;
            }
            
            // 3. Check attempt limit
            int currentAttempts = user.getOtpAttempts() != null ? user.getOtpAttempts() : 0;
            if (currentAttempts >= 3) {
                otpAuditService.updateOtpFailed(req.getPhone(), currentAttempts);
                clearOtpData(user);
                log.warn("Max OTP attempts exceeded for phone: {}", maskedPhone);
                return null;
            }
            
            // 4. Verify OTP
            if (!isOtpValid(req.getOtp(), user.getOtpCode())) {
                user.setOtpAttempts(currentAttempts + 1);
                users.save(user);
                otpAuditService.updateOtpFailed(req.getPhone(), currentAttempts + 1);
                log.warn("Invalid OTP attempt {} for phone: {}", currentAttempts + 1, maskedPhone);
                return null;
            }
            
            // 5. Success - clear OTP and mark verified
            otpAuditService.updateOtpVerified(req.getPhone(), "****");
            clearOtpData(user);
            
            // 6. Handle password reset purpose
            if ("password_reset".equals(req.getPurpose())) {
                String randomPassword = generateRandomPassword();
                user.setPasswordHash(encoder.encode(randomPassword));
                users.save(user);
                log.info("Password reset with random password for user: {}", maskedPhone);
                return buildTokens(user, req.getMerchantId());
            }
            
            log.info("OTP verified successfully for phone: {}", maskedPhone);
            return buildTokens(user, req.getMerchantId());
            
        } catch (Exception e) {
            log.error("OTP verification error for phone: {}, error: {}", maskedPhone, sanitizeForLogging(e.getMessage()), e);
            return null;
        }
    }
    

    
    @Override
    @Transactional
    public void resetPassword(PasswordResetRequest req) {
        User user = findUserForOtp(new OtpRequest(req.getPhone(), req.getMerchantId()));
        
        OtpVerifyRequest otpVerifyReq = new OtpVerifyRequest();
        otpVerifyReq.setPhone(req.getPhone());
        otpVerifyReq.setMerchantId(req.getMerchantId());
        otpVerifyReq.setOtp(req.getOtp());
        
        if (!verifyOtp(otpVerifyReq)) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }
        
        if (!req.getNewPassword().matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$")) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character");
        }
        
        user.setPasswordHash(encoder.encode(req.getNewPassword()));
        users.save(user);
        
        // Log password reset completion
        log.info("Password reset completed for user: {}", maskPhoneNumber(req.getPhone()));
    }
}