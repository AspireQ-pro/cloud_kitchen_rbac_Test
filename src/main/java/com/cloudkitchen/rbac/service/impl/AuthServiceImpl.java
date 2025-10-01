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
import com.cloudkitchen.rbac.dto.auth.AuthRequest;
import com.cloudkitchen.rbac.dto.auth.AuthResponse;
import com.cloudkitchen.rbac.dto.auth.OtpRequest;
import com.cloudkitchen.rbac.dto.auth.OtpVerifyRequest;
import com.cloudkitchen.rbac.dto.auth.RefreshTokenRequest;
import com.cloudkitchen.rbac.dto.auth.RegisterRequest;
import com.cloudkitchen.rbac.enums.UserType;
import com.cloudkitchen.rbac.repository.MerchantRepository;
import com.cloudkitchen.rbac.repository.OtpLogRepository;
import com.cloudkitchen.rbac.repository.RoleRepository;
import com.cloudkitchen.rbac.repository.UserRepository;
import com.cloudkitchen.rbac.repository.UserRoleRepository;
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
    public AuthServiceImpl(UserRepository users, MerchantRepository merchants, RoleRepository roles,
            UserRoleRepository userRoles, PasswordEncoder encoder, OtpService otpService,
            OtpAuditService otpAuditService, SmsService smsService, JwtTokenProvider jwt,
            OtpLogRepository otpLogRepository) {
        this.users = users;
        this.merchants = merchants;
        this.roles = roles;
        this.userRoles = userRoles;
        this.encoder = encoder;
        this.otpService = otpService;
        this.otpAuditService = otpAuditService;
        this.smsService = smsService;
        this.jwt = jwt;
        this.otpLogRepository = otpLogRepository;
    }

    @Override
    @Transactional
    public AuthResponse registerUser(RegisterRequest req) {
        log.info("Customer registration attempt: merchantId={}", req.getMerchantId());
        validateRegistrationRequest(req);
        
        // Check if phone already exists for this merchant
        if (users.findByPhoneAndMerchant_MerchantId(req.getPhone(), req.getMerchantId()).isPresent()) {
            throw new RuntimeException("Phone number " + maskPhoneNumber(req.getPhone()) + " is already registered for this merchant. Please use a different phone number or login instead.");
        }
        
        // Get existing merchant (must exist)
        Merchant merchant = merchants.findById(req.getMerchantId())
                .orElseThrow(() -> new RuntimeException("Merchant with ID " + req.getMerchantId() + " not found. Please contact support."));
        
        // Create customer user
        User user = createUser(req, merchant, "customer");
        user = users.save(user);
        log.debug("Customer created successfully with ID: {}", user.getUserId());
        
        // Assign customer role
        assignUserRole(user, "customer", merchant);
        log.debug("Customer role assigned successfully for user: {}", user.getUserId());
        
        return buildTokens(user, merchant.getMerchantId());
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
        
        // Set email if provided in request
        if (req.getEmail() != null && !req.getEmail().trim().isEmpty()) {
            user.setEmail(req.getEmail().trim());
        }
        
        return user;
    }
    
    private void assignUserRole(User user, String userType, Merchant merchant) {
        String roleName = "super_admin".equals(userType) ? "super_admin" : "customer";

        log.debug("User role assignment: userType={}, roleName={}", userType, roleName);

        Role role = roles.findByRoleName(roleName)
                .orElseThrow(() -> new IllegalStateException(roleName + " role not found. Please ensure roles are properly initialized."));

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        userRole.setMerchant(merchant);
        userRole.setAssignedAt(LocalDateTime.now());
        userRoles.save(userRole);

        log.info("Successfully assigned role '{}' to user '{}' for merchant '{}'",
                roleName, user.getUserId(), merchant != null ? merchant.getMerchantId() : "null");
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
    
    private User findUserByPhoneAndMerchantId(String phone, Integer merchantId) {
        try {
            if (merchantId == null || merchantId == 0) {
                // For merchantId=0, find admin/merchant users (merchant_id is null)
                // Also try to find by phone number only if no admin/merchant found
                return users.findByPhoneAndMerchantIsNull(phone)
                        .or(() -> {
                            // If no admin/merchant user found, try to find any user by phone
                            // This allows OTP generation for merchantId=0 with any phone number
                            log.debug("No admin/merchant user found, trying to find any user with phone: {}", maskPhoneNumber(phone));
                            return users.findByPhone(phone).stream().findFirst();
                        })
                        .orElseThrow(() -> new RuntimeException("No user found with phone: " + maskPhoneNumber(phone)));
            } else {
                // For merchantId>0, find customer users for specific merchant
                return users.findByPhoneAndMerchant_MerchantId(phone, merchantId)
                        .orElseThrow(() -> new RuntimeException("No customer found with phone: " + maskPhoneNumber(phone) + " for merchant: " + merchantId));
            }
        } catch (Exception e) {
            log.error("Database error while finding user: phone={}, merchantId={}, error={}", 
                     maskPhoneNumber(phone), merchantId, e.getMessage());
            throw new RuntimeException("User lookup failed: " + e.getMessage(), e);
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
        List<String> permissionNames = userRoles.findPermissionNames(user.getUserId(), actualMerchantId);

        String accessToken = jwt.createAccessToken(user.getUserId(), actualMerchantId, roleNames, permissionNames);
        String refreshToken = jwt.createRefreshToken(user.getUserId(), actualMerchantId);

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

    
    private boolean isOtpValid(String inputOtp, String storedOtp) {
        if (inputOtp == null || storedOtp == null) return false;
        // SECURITY NOTE: Plain text comparison for development stage
        // In production, use otpService.verifyOtp(inputOtp, storedOtp) for hashed comparison
        return otpService.verifyOtp(inputOtp, storedOtp);
    }
    

    

    
    private String maskPhoneNumber(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return "****" + phone.substring(phone.length() - 4);
    }
    
    private void validateUserRoleForOtp(User user, Integer merchantId) {
        if (merchantId == null || merchantId == 0) {
            // For merchantId=0, send OTP to phone number (any user type allowed)
            log.debug("OTP request with merchantId=0 for phone: {}, allowing OTP generation", maskPhoneNumber(user.getPhone()));
        } else {
            // For merchantId>0, user must be customer
            if (!"customer".equals(user.getUserType())) {
                throw new RuntimeException("Access denied. Only customers can request OTP for specific merchant.");
            }
            
            // Verify customer is associated with this merchant
            if (!isUserAssociatedWithMerchant(user.getUserId(), merchantId)) {
                throw new RuntimeException("Access denied. User is not associated with the given merchant.");
            }
        }
    }
    
    private boolean isUserAssociatedWithMerchant(Integer userId, Integer merchantId) {
        try {
            return userRoles.existsByUser_UserIdAndMerchant_MerchantId(userId, merchantId);
        } catch (Exception e) {
            log.warn("Error checking user-merchant association: userId={}, merchantId={}, error={}", 
                    userId, merchantId, e.getMessage());
            return false;
        }
    }
    
    private boolean isPhoneBlocked(String phone, Integer merchantId) {
        try {
            User user = findUserByPhoneAndMerchantId(phone, merchantId);
            LocalDateTime blockedUntil = user.getOtpBlockedUntil();
            boolean isBlocked = blockedUntil != null && blockedUntil.isAfter(LocalDateTime.now());
            
            if (isBlocked) {
                log.warn("Phone {} is blocked until: {}", maskPhoneNumber(phone), blockedUntil);
            }
            
            return isBlocked;
        } catch (Exception e) {
            log.warn("Error checking phone block status: phone={}, merchantId={}, error={}", 
                    maskPhoneNumber(phone), merchantId, e.getMessage());
            // If we can't check block status, assume not blocked to allow OTP generation
            return false;
        }
    }
    
    private void validateOtpRateLimit(String phone, String otpType) {
        LocalDateTime timeLimit = LocalDateTime.now().minusMinutes(30); // 30-minute window
        int maxRequests = 3; // Allow 3 requests
        int recentRequests = otpLogRepository.countByPhoneAndCreatedOnAfter(phone, timeLimit);
        
        if (recentRequests >= maxRequests) {
            log.warn("Rate limit exceeded for phone: {}, type: {}, requests: {} in last 30 minutes", 
                    maskPhoneNumber(phone), otpType, recentRequests);
            throw new RuntimeException("Too many OTP requests. You have exceeded the limit of 3 OTP requests. Please try again after 30 minutes.");
        }
    }
    
    private void invalidateExistingOtp(User user, String otpType) {
        if (user.getOtpCode() != null) {
            // Just clear the OTP data, don't update audit logs
            clearOtpData(user);
            log.debug("Cleared existing OTP for user: {}", user.getUserId());
        }
    }
    
    private String generateOtp() {
        String otp = otpService.generateOtp();
        // SECURITY NOTE: OTP is generated and stored in plain text for development
        // In production, consider using otpService.hashOtp(otp) for storage
        return otp;
    }
    
    private LocalDateTime getExpiryByType(String otpType) {
        return switch (otpType) {
            case "password_reset", "phone_verification", "account_verification" -> LocalDateTime.now().plusMinutes(5);
            default -> LocalDateTime.now().plusMinutes(5);
        };
    }
    

    
    private boolean sendOtpByType(String phone, String otpCode) {
        return smsService.sendOtp(phone, otpCode);
    }
    

    
    private void clearOtpData(User user) {
        user.setOtpCode(null);
        user.setOtpExpiresAt(null);
        user.setOtpAttempts(0);
        users.save(user);
    }
    
    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@$!%*?&";
        java.security.SecureRandom random = new java.security.SecureRandom();
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
        try {
            // Blacklist current token if available
            org.springframework.security.core.Authentication auth = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getCredentials() instanceof String) {
                String token = (String) auth.getCredentials();
                jwt.blacklistToken(token);
                log.debug("Token blacklisted for user: {}", userId);
            }
            
            // Clear security context
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
            
            log.info("User {} logged out successfully with token invalidation", userId);
        } catch (Exception e) {
            log.error("Error during logout for user {}: {}", userId, e.getMessage());
            throw new RuntimeException("Logout failed");
        }
    }
    
    @Override
    @Transactional
    public void requestOtp(OtpRequest req) {
        String maskedPhone = maskPhoneNumber(req.getPhone());
        String otpType = req.getOtpType() != null ? req.getOtpType() : "login";
        log.info("OTP request for phone: {}, merchantId: {}, type: {}", maskedPhone, req.getMerchantId(), otpType);
        
        try {
            // 1. Validate input
            if (req.getPhone() == null || req.getPhone().trim().isEmpty()) {
                throw new IllegalArgumentException("Phone number is required");
            }
            
            // Validate phone number format
            String cleanPhone = req.getPhone().trim().replaceAll("[^0-9]", "");
            if (!cleanPhone.matches("^[6-9]\\d{9}$")) {
                throw new IllegalArgumentException("Phone number must be a valid 10-digit Indian mobile number starting with 6-9");
            }
            
            // Validate OTP type
            if (req.getOtpType() != null && !req.getOtpType().matches("^(login|password_reset|registration|phone_verification)$")) {
                throw new IllegalArgumentException("Invalid OTP type. Must be one of: login, password_reset, registration, phone_verification");
            }
            
            // 2. Find user by phone and merchant context
            User user;
            try {
                user = findUserByPhoneAndMerchantId(req.getPhone(), req.getMerchantId());
                log.debug("Found user for OTP request: userId={}, userType={}, merchantId={}", 
                         user.getUserId(), user.getUserType(), 
                         user.getMerchant() != null ? user.getMerchant().getMerchantId() : "null");
            } catch (RuntimeException e) {
                log.error("User not found for OTP request: phone={}, merchantId={}, error={}", 
                         maskedPhone, req.getMerchantId(), e.getMessage());
                // For password_reset, user must exist
                if ("password_reset".equals(otpType)) {
                    throw new RuntimeException("User not found. Please register first or check your phone number and merchant ID.");
                }
                // For other types, could potentially create user (but not implemented yet)
                throw new RuntimeException("User not found for the provided phone number and merchant. Please register first.");
            }
            
            // 2.1. Validate user role and merchant association
            try {
                validateUserRoleForOtp(user, req.getMerchantId());
                log.debug("User role validation passed for userId={}, userType={}, merchantId={}", 
                         user.getUserId(), user.getUserType(), req.getMerchantId());
            } catch (RuntimeException e) {
                log.warn("Role validation failed for user: userId={}, userType={}, merchantId={}, error={}", 
                        user.getUserId(), user.getUserType(), req.getMerchantId(), e.getMessage());
                throw e;
            }
            
            // 2.2. Check if phone is blocked due to failed OTP attempts
            try {
                if (isPhoneBlocked(req.getPhone(), req.getMerchantId())) {
                    log.warn("Phone blocked for OTP requests: phone={}, merchantId={}", 
                            maskedPhone, req.getMerchantId());
                    throw new RuntimeException("Phone is blocked due to too many failed OTP attempts. Please try again later.");
                }
            } catch (RuntimeException e) {
                if (e.getMessage().contains("blocked")) {
                    throw e;
                }
                log.warn("Error checking phone block status: phone={}, continuing: {}", 
                        maskedPhone, e.getMessage());
            }
            
            // 3. Check rate limiting based on OTP type
            try {
                validateOtpRateLimit(req.getPhone(), otpType);
            } catch (RuntimeException e) {
                log.warn("Rate limit validation failed for phone: {}, type: {}, error: {}", 
                        maskedPhone, otpType, e.getMessage());
                throw e;
            }
            
            // 4. Invalidate any existing OTP
            try {
                invalidateExistingOtp(user, otpType);
            } catch (Exception e) {
                log.warn("Failed to invalidate existing OTP for phone: {}, continuing: {}", 
                        maskedPhone, e.getMessage());
            }
            
            // 5. Generate secure OTP
            String otpCode = generateOtp();
            LocalDateTime expiresAt = getExpiryByType(otpType);
            log.debug("Generated OTP for phone: {}, expires at: {}", maskedPhone, expiresAt);
            
            // 6. Store OTP securely
            user.setOtpCode(otpCode);
            user.setOtpExpiresAt(expiresAt);
            user.setOtpAttempts(0);
            users.save(user);
            log.debug("OTP stored successfully for user: {}", user.getUserId());
            
            // 7. Send OTP via SMS
            boolean smsSent = sendOtpByType(req.getPhone(), otpCode);
            String status = smsSent ? "sent" : "send_failed";
            log.debug("SMS send result for phone: {}, status: {}", maskedPhone, status);
            
            // 8. Audit log with actual OTP type
            try {
                Integer merchantId = user.getMerchant() != null ? user.getMerchant().getMerchantId() : req.getMerchantId();
                otpAuditService.logOtp(merchantId, req.getPhone(), otpCode, otpType, status, expiresAt);
                log.debug("OTP audit log created for phone: {}", maskedPhone);
            } catch (Exception e) {
                log.warn("Failed to create OTP audit log for phone: {}, error: {}", maskedPhone, e.getMessage());
                // Don't fail the entire operation for audit logging issues
            }
            
            if (!smsSent) {
                throw new RuntimeException("SMS service temporarily unavailable. Please try again.");
            }
            
            log.info("OTP sent successfully to phone: {}, type: {}", maskedPhone, otpType);
            
        } catch (Exception e) {
            log.error("OTP request failed for phone: {}, type: {}, error: {}", 
                     maskedPhone, otpType, sanitizeForLogging(e.getMessage()), e);
            if (e instanceof RuntimeException) {
                throw e;
            } else {
                throw new RuntimeException("Failed to process OTP request: " + e.getMessage(), e);
            }
        }
    }
    
    @Override
    public AuthResponse verifyOtp(OtpVerifyRequest req) {
        String maskedPhone = maskPhoneNumber(req.getPhone());
        log.info("OTP verification attempt for phone: {}, otpType: {}", maskedPhone, req.getOtpType());
        
        try {
            User user = findUserByPhoneAndMerchantId(req.getPhone(), req.getMerchantId());
            
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
            Integer otpAttempts = user.getOtpAttempts();
            int currentAttempts = otpAttempts != null ? otpAttempts : 0;
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
            Integer merchantId = user.getMerchant() != null ? user.getMerchant().getMerchantId() : req.getMerchantId();
            otpAuditService.logOtpVerified(req.getPhone(), merchantId);
            clearOtpData(user);
            
            // 6. Handle password reset otpType
            if ("password_reset".equals(req.getOtpType())) {
                String randomPassword = generateRandomPassword();
                user.setPasswordHash(encoder.encode(randomPassword));
                users.save(user);
                log.info("Password reset with random password for user: {}", maskedPhone);
                return buildTokens(user, req.getMerchantId());
            }
            
            log.info("OTP verified successfully for phone: {}", maskedPhone);
            return buildTokens(user, req.getMerchantId());
            
        } catch (RuntimeException e) {
            log.error("OTP verification error for phone: {}, error: {}", maskedPhone, sanitizeForLogging(e.getMessage()), e);
            return null;
        }
    }
    

    

    
    @Override
    public long getUserCount() {
        return users.count();
    }
}