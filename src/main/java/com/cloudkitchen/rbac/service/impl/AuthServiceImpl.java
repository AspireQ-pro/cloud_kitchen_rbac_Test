package com.cloudkitchen.rbac.service.impl;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cloudkitchen.rbac.config.AppConstants;
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
import com.cloudkitchen.rbac.service.ValidationService;

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
    private final ValidationService validationService;

    public AuthServiceImpl(UserRepository users, MerchantRepository merchants, RoleRepository roles,
            UserRoleRepository userRoles, PasswordEncoder encoder, OtpService otpService,
            OtpAuditService otpAuditService, SmsService smsService, JwtTokenProvider jwt,
            OtpLogRepository otpLogRepository, ValidationService validationService) {
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
        this.validationService = validationService;
    }

    private Integer getEffectiveMerchantId(User user, Integer requestMerchantId) {
        if (requestMerchantId != null) {
            return requestMerchantId;
        }
        return user.getMerchant() != null && user.getMerchant().getMerchantId() != null ?
               user.getMerchant().getMerchantId() : null;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return "****";
        }
        if (phone.length() < 4) {
            return "*".repeat(phone.length());
        }
        return "****" + phone.substring(phone.length() - 4);
    }

    @Override
    @Transactional
    public AuthResponse registerUser(RegisterRequest req) {
        if (req.getMerchantId() == null || req.getMerchantId() <= 0) {
            throw new IllegalArgumentException("Valid merchantId (>0) is required for customer registration");
        }
        
        log.info("Customer registration attempt: merchantId={}", req.getMerchantId());
        validationService.validateRegistration(req);
        
        if (users.findByPhoneAndMerchant_MerchantId(req.getPhone(), req.getMerchantId()).isPresent()) {
            throw new RuntimeException("Phone number " + maskPhone(req.getPhone()) + " is already registered for this merchant. Please use a different phone number or login instead.");
        }
        
        Merchant merchant = merchants.findById(req.getMerchantId())
                .orElseThrow(() -> new RuntimeException("Merchant with ID " + req.getMerchantId() + " not found. Please contact support."));
        
        User user = createUser(req, merchant, "customer");
        user = users.save(user);
        
        assignUserRole(user, "customer", merchant);
        
        return buildTokens(user, merchant.getMerchantId());
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
        
        if (req.getEmail() != null && !req.getEmail().trim().isEmpty()) {
            user.setEmail(req.getEmail().trim());
        }
        
        return user;
    }
    
    private void assignUserRole(User user, String userType, Merchant merchant) {
        String roleName = switch (userType) {
            case "super_admin" -> "super_admin";
            case "merchant" -> "merchant";
            default -> "customer";
        };

        Role role = roles.findByRoleName(roleName)
                .orElseThrow(() -> new IllegalStateException(roleName + " role not found. Please ensure roles are properly initialized."));

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        userRole.setMerchant(merchant);
        userRole.setAssignedAt(LocalDateTime.now());
        userRoles.save(userRole);
    }

    @Override
    public AuthResponse login(AuthRequest req) {
        log.info("Login attempt: username={}, merchantId={}", sanitizeForLogging(req.getUsername()), req.getMerchantId());
        
        // Validate input
        if (req.getUsername() == null || req.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }
        if (req.getPassword() == null || req.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (req.getMerchantId() == null) {
            throw new IllegalArgumentException("MerchantId is required: use 0 for admin/merchant, >0 for customers");
        }
        
        User user;
        
        if (req.getMerchantId().equals(0)) {
            // Find user by username (admin or merchant)
            user = users.findByUsername(req.getUsername())
                .filter(u -> "merchant".equals(u.getUserType()) || "super_admin".equals(u.getUserType()))
                .orElseThrow(() -> new RuntimeException("User not found. Please check your credentials."));
            
            // Check if user is active
            if (!Boolean.TRUE.equals(user.getActive())) {
                throw new RuntimeException("Account is inactive. Please contact support.");
            }
        }
        else if (req.getMerchantId() > 0) {
            user = users.findByPhoneAndMerchant_MerchantId(req.getUsername(), req.getMerchantId())
                    .orElseThrow(() -> new RuntimeException("Customer not found for this merchant."));
        }
        else {
            throw new IllegalArgumentException("Invalid merchantId. Use 0 for admin/merchant, >0 for customers");
        }
        
        // Verify password
        if (user.getPasswordHash() == null || user.getPasswordHash().trim().isEmpty()) {
            throw new RuntimeException("Account setup incomplete. Please contact support.");
        }
        
        if (!encoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid password.");
        }
        
        log.info("Login successful for user: {} (type: {})", user.getUserId(), user.getUserType());
        return buildTokens(user, req.getMerchantId());
    }
    
    private User findUserByPhoneAndMerchantId(String phone, Integer merchantId) {
        try {
            if (merchantId == null || merchantId == 0) {
                return users.findByPhoneAndMerchantIsNull(phone)
                        .or(() -> {
                            log.debug("No admin/merchant user found, trying to find any user with phone: {}", maskPhone(phone));
                            return users.findByPhone(phone).stream().findFirst();
                        })
                        .orElseThrow(() -> new RuntimeException("No user found with phone: " + maskPhone(phone)));
            } else {
                return users.findByPhoneAndMerchant_MerchantId(phone, merchantId)
                        .orElseThrow(() -> new RuntimeException("No customer found with phone: " + maskPhone(phone) + " for merchant: " + merchantId));
            }
        } catch (Exception e) {
            log.error("Database error while finding user: phone={}, merchantId={}", maskPhone(phone), merchantId, e);
            throw new RuntimeException("User lookup failed", e);
        }
    }

    @Override
    public AuthResponse refresh(RefreshTokenRequest req) {
        if (req == null || req.getRefreshToken() == null || req.getRefreshToken().trim().isEmpty()) {
            throw new IllegalArgumentException("Refresh token is required");
        }
        
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
            throw new RuntimeException("Invalid refresh token", e);
        }
    }

    private AuthResponse buildTokens(User user, Integer merchantId) {
        // Always return the user's actual merchantId from their entity
        Integer actualMerchantId = user.getMerchant() != null && user.getMerchant().getMerchantId() != null ? 
                user.getMerchant().getMerchantId() : merchantId;
        
        // For superadmin users, use null instead of 0 for merchant queries
        Integer queryMerchantId = (actualMerchantId != null && actualMerchantId == 0) ? null : actualMerchantId;
        
        List<String> roleNames = userRoles.findRoleNames(user.getUserId(), queryMerchantId);
        List<String> permissionNames = userRoles.findPermissionNames(user.getUserId(), queryMerchantId);
        
        // Handle users without roles - assign default role based on userType
        if (roleNames == null || roleNames.isEmpty()) {
            String defaultRole = getDefaultRoleForUserType(user.getUserType());
            roleNames = List.of(defaultRole);
        }
        
        // Ensure permissions list is not null
        if (permissionNames == null) {
            permissionNames = List.of();
        }

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
        return otpService.verifyOtp(inputOtp, storedOtp);
    }
    
    private void validateUserRoleForOtp(User user, Integer merchantId) {
        if (merchantId == null || merchantId == 0) {
            log.debug("OTP request with merchantId=0 for phone: {}, allowing OTP generation", maskPhone(user.getPhone()));
        } else {
            if (!"customer".equals(user.getUserType())) {
                throw new RuntimeException("Access denied. Only customers can request OTP for specific merchant.");
            }
            
            if (!isUserAssociatedWithMerchant(user.getUserId(), merchantId)) {
                throw new RuntimeException("Access denied. User is not associated with the given merchant.");
            }
        }
    }
    
    private boolean isUserAssociatedWithMerchant(Integer userId, Integer merchantId) {
        if (userId == null || merchantId == null) {
            return false;
        }
        
        try {
            return userRoles.existsByUser_UserIdAndMerchant_MerchantId(userId, merchantId);
        } catch (Exception e) {
            log.warn("Error checking user-merchant association: userId={}, merchantId={}", userId, merchantId, e);
            return false;
        }
    }
    
    private boolean isPhoneBlocked(String phone, Integer merchantId) {
        try {
            User user = findUserByPhoneAndMerchantId(phone, merchantId);
            LocalDateTime blockedUntil = user.getOtpBlockedUntil();
            boolean isBlocked = blockedUntil != null && blockedUntil.isAfter(LocalDateTime.now());
            
            if (isBlocked) {
                log.warn("Phone {} is blocked until: {}", maskPhone(phone), blockedUntil);
            }
            
            return isBlocked;
        } catch (Exception e) {
            log.warn("Error checking phone block status: phone={}, merchantId={}", maskPhone(phone), merchantId, e);
            return false;
        }
    }
    
    private void validateOtpRateLimit(String phone, String otpType) {
        int windowMinutes = AppConstants.OTP_RATE_LIMIT_WINDOW_MINUTES;
        LocalDateTime timeLimit = LocalDateTime.now().minusMinutes(windowMinutes);
        int recentRequests = otpLogRepository.countByPhoneAndCreatedOnAfter(phone, timeLimit);
        
        int maxRequests = AppConstants.OTP_RATE_LIMIT_REQUESTS;
        if (recentRequests >= maxRequests) {
            log.warn("Rate limit exceeded for phone: {}, type: {}, requests: {} in last {} minutes", 
                    maskPhone(phone), otpType, recentRequests, windowMinutes);
            
            String errorMessage = String.format(
                "Too many OTP requests. You have exceeded the limit of %d OTP requests. Please try again after %d minutes.", 
                maxRequests, 
                windowMinutes
            );
            throw new RuntimeException(errorMessage);
        }
    }
    
    private void invalidateExistingOtp(User user) {
        if (user.getOtpCode() != null) {
            clearOtpData(user);
            log.debug("Cleared existing OTP for user: {}", user.getUserId());
        }
    }
    
    private String generateOtp() {
        return otpService.generateOtp();
    }
    
    private LocalDateTime getExpiryByType(String otpType) {
        int expiryMinutes = AppConstants.OTP_EXPIRY_MINUTES;
        return switch (otpType) {
            case "password_reset", "phone_verification", "account_verification" -> LocalDateTime.now().plusMinutes(expiryMinutes);
            default -> LocalDateTime.now().plusMinutes(expiryMinutes);
        };
    }
    
    private boolean sendOtpByType(String phone, String otpCode) {
        return smsService.sendOtp(phone, otpCode);
    }
    
    private void clearOtpData(User user) {
        if (user == null) {
            log.warn("Attempted to clear OTP data for null user");
            return;
        }
        
        user.setOtpCode(null);
        user.setOtpExpiresAt(null);
        user.setOtpAttempts(0);
        users.save(user);
    }
    
    private String generateRandomPassword() {
        final char[] upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
        final char[] lowerCase = "abcdefghijklmnopqrstuvwxyz".toCharArray();
        final char[] digits = "0123456789".toCharArray();
        final char[] specialChars = "@$!%*?&".toCharArray();
        
        SecureRandom random = new SecureRandom();
        char[] password = new char[10];
        
        // Ensure at least one character from each category
        password[0] = upperCase[random.nextInt(upperCase.length)];
        password[1] = lowerCase[random.nextInt(lowerCase.length)];
        password[2] = digits[random.nextInt(digits.length)];
        password[3] = specialChars[random.nextInt(specialChars.length)];
        
        // Fill remaining positions with random characters from all categories
        char[][] allCategories = {upperCase, lowerCase, digits, specialChars};
        for (int i = 4; i < 10; i++) {
            char[] category = allCategories[random.nextInt(allCategories.length)];
            password[i] = category[random.nextInt(category.length)];
        }
        
        // Shuffle the password to randomize character positions
        return shuffleString(new String(password), random);
    }
    
    private String shuffleString(String input, SecureRandom random) {
        char[] array = input.toCharArray();
        for (int i = array.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
        return new String(array);
    }
    
    private String getDefaultRoleForUserType(String userType) {
        return switch (userType) {
            case "super_admin" -> "super_admin";
            case "merchant" -> "merchant";
            case "customer" -> "customer";
            default -> "customer";
        };
    }
    
    private String sanitizeForLogging(String input) {
        if (input == null) {
            return "null";
        }
        // Prevent log injection by removing CRLF and control characters
        String sanitized = input.replaceAll("[\r\n\t\f\u0008\u001B]", "_")
                               .replaceAll("[\u0000-\u001F\u007F-\u009F]", "_")
                               .replaceAll("[^\\w\\s@.-]", "");
        return sanitized.length() > 100 ? sanitized.substring(0, 100) + "..." : sanitized;
    }
    


    @Override
    @Transactional
    public void logout(Integer userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        try {
            org.springframework.security.core.Authentication auth = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getCredentials() instanceof String token) {
                jwt.blacklistToken(token);
                log.debug("Token blacklisted for user: {}", userId);
            }
            
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
            
            log.info("User {} logged out successfully with token invalidation", userId);
        } catch (Exception e) {
            log.error("Error during logout for user {}: {}", userId, sanitizeForLogging(e.getMessage()));
            throw new RuntimeException("Logout failed");
        }
    }
    
    @Override
    @Transactional
    public void requestOtp(OtpRequest req) {
        String maskedPhone = maskPhone(req.getPhone());
        String otpType = req.getOtpType() != null ? req.getOtpType() : "login";
        log.info("OTP request for phone: {}, merchantId: {}, type: {}", maskedPhone, req.getMerchantId(), otpType);
        
        try {
            if (req.getPhone() == null || req.getPhone().trim().isEmpty()) {
                throw new IllegalArgumentException("Phone number is required");
            }
            
            String cleanPhone = req.getPhone().trim().replaceAll("[^0-9]", "");
            validationService.validatePhone(cleanPhone);
            
            if (req.getOtpType() != null) {
                String otpTypeToValidate = req.getOtpType();
                if (!"login".equals(otpTypeToValidate) && 
                    !"password_reset".equals(otpTypeToValidate) && 
                    !"registration".equals(otpTypeToValidate) && 
                    !"phone_verification".equals(otpTypeToValidate)) {
                    throw new IllegalArgumentException("Invalid OTP type. Must be one of: login, password_reset, registration, phone_verification");
                }
            }
            
            User user;
            try {
                user = findUserByPhoneAndMerchantId(req.getPhone(), req.getMerchantId());
                log.debug("Found user for OTP request: userId={}, userType={}, merchantId={}", 
                         user.getUserId(), user.getUserType(), 
                         user.getMerchant() != null ? user.getMerchant().getMerchantId() : "null");
            } catch (RuntimeException e) {
                log.error("User not found for OTP request: phone={}, merchantId={}, error={}", 
                         maskedPhone, req.getMerchantId(), e.getMessage());
                if ("password_reset".equals(otpType)) {
                    throw new RuntimeException("User not found. Please register first or check your phone number and merchant ID.");
                }
                throw new RuntimeException("User not found for the provided phone number and merchant. Please register first.");
            }
            
            try {
                validateUserRoleForOtp(user, req.getMerchantId());
                log.debug("User role validation passed for userId={}, userType={}, merchantId={}", 
                         user.getUserId(), user.getUserType(), req.getMerchantId());
            } catch (RuntimeException e) {
                log.warn("Role validation failed for user: userId={}, userType={}, merchantId={}, error={}", 
                        user.getUserId(), user.getUserType(), req.getMerchantId(), e.getMessage());
                throw e;
            }
            
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
            
            try {
                validateOtpRateLimit(req.getPhone(), otpType);
            } catch (RuntimeException e) {
                log.warn("Rate limit validation failed for phone: {}, type: {}, error: {}", 
                        maskedPhone, otpType, e.getMessage());
                throw e;
            }
            
            try {
                invalidateExistingOtp(user);
            } catch (Exception e) {
                log.warn("Failed to invalidate existing OTP for phone: {}, continuing: {}", 
                        maskedPhone, e.getMessage());
            }
            
            String otpCode = generateOtp();
            LocalDateTime expiresAt = getExpiryByType(otpType);
            log.debug("Generated OTP for phone: {}, expires at: {}", maskedPhone, expiresAt);
            
            user.setOtpCode(otpCode);
            user.setOtpExpiresAt(expiresAt);
            user.setOtpAttempts(0);
            users.save(user);
            log.debug("OTP stored successfully for user: {}", user.getUserId());
            
            boolean smsSent = sendOtpByType(req.getPhone(), otpCode);
            String status = smsSent ? "sent" : "send_failed";
            log.debug("SMS send result for phone: {}, status: {}", maskedPhone, status);
            
            try {
                Integer merchantId = user.getMerchant() != null && user.getMerchant().getMerchantId() != null ? user.getMerchant().getMerchantId() : req.getMerchantId();
                otpAuditService.logOtp(merchantId, req.getPhone(), otpCode, otpType, status, expiresAt);
                log.debug("OTP audit log created for phone: {}", maskedPhone);
            } catch (Exception e) {
                log.warn("Failed to create OTP audit log for phone: {}, error: {}", maskedPhone, e.getMessage());
            }
            
            if (!smsSent) {
                throw new RuntimeException("SMS service temporarily unavailable. Please try again.");
            }
            
            log.info("OTP sent successfully to phone: {}, type: {}", maskedPhone, otpType);
            
        } catch (RuntimeException e) {
            log.error("OTP request failed for phone: {}, type: {}", maskedPhone, otpType, e);
            throw e;
        } catch (Exception e) {
            log.error("OTP request failed for phone: {}, type: {}", maskedPhone, otpType, e);
            throw new RuntimeException("Failed to process OTP request", e);
        }
    }
    
    @Override
    public AuthResponse verifyOtp(OtpVerifyRequest req) {
        String maskedPhone = maskPhone(req.getPhone());
        log.info("OTP verification attempt for phone: {}, otpType: {}", maskedPhone, req.getOtpType());
        
        try {
            User user = findUserByPhoneAndMerchantId(req.getPhone(), req.getMerchantId());
            
            if (user.getOtpCode() == null) {
                log.warn("No active OTP found for phone: {}", maskedPhone);
                return null;
            }
            
            if (user.getOtpExpiresAt() == null || user.getOtpExpiresAt().isBefore(LocalDateTime.now().minusSeconds(30))) {
                otpAuditService.updateOtpExpired(req.getPhone());
                clearOtpData(user);
                log.warn("Expired OTP verification attempt for phone: {}", maskedPhone);
                return null;
            }
            
            Integer otpAttempts = user.getOtpAttempts();
            int currentAttempts = otpAttempts != null ? otpAttempts : 0;
            int maxAttempts = AppConstants.OTP_MAX_ATTEMPTS;
            if (currentAttempts >= maxAttempts) {
                otpAuditService.updateOtpFailed(req.getPhone(), currentAttempts);
                clearOtpData(user);
                log.warn("Max OTP attempts exceeded for phone: {}", maskedPhone);
                return null;
            }
            
            if (!isOtpValid(req.getOtp(), user.getOtpCode())) {
                user.setOtpAttempts(currentAttempts + 1);
                users.save(user);
                otpAuditService.updateOtpFailed(req.getPhone(), currentAttempts + 1);
                log.warn("Invalid OTP attempt {} for phone: {}", currentAttempts + 1, maskedPhone);
                return null;
            }
            
            Integer merchantId = user.getMerchant() != null && user.getMerchant().getMerchantId() != null ? user.getMerchant().getMerchantId() : req.getMerchantId();
            otpAuditService.logOtpVerified(req.getPhone(), merchantId);
            clearOtpData(user);
            
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
            log.error("OTP verification error for phone: {}", maskedPhone, e);
            return null;
        }
    }
    
    @Override
    public long getUserCount() {
        return users.count();
    }
}