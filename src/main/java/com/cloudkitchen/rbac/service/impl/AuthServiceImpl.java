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
import com.cloudkitchen.rbac.exception.BusinessExceptions.*;

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

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return "****" + phone.substring(phone.length() - 4);
    }

    @Override
    @Transactional
    public AuthResponse registerUser(RegisterRequest req) {
        if (req.getMerchantId() == null || req.getMerchantId() <= 0) {
            throw new IllegalArgumentException("Valid merchantId (>0) is required for customer registration");
        }
        
        log.info("Customer registration attempt for merchantId: {}", req.getMerchantId());
        validationService.validateRegistration(req);
        
        if (users.findByPhoneAndMerchant_MerchantId(req.getPhone(), req.getMerchantId()).isPresent()) {
            throw new UserAlreadyExistsException("Phone number " + maskPhone(req.getPhone()) + " is already registered for this merchant. Please use a different phone number or login instead.");
        }
        
        Merchant merchant = merchants.findById(req.getMerchantId())
                .orElseThrow(() -> new MerchantNotFoundException("Merchant with ID " + req.getMerchantId() + " not found. Please contact support."));
        
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
        log.info("Login attempt for merchantId: {}", req.getMerchantId());
        
        // Validate input - check for empty request
        if (req.getUsername() == null && req.getPassword() == null && req.getMerchantId() == null) {
            throw new IllegalArgumentException("Missing required fields: merchantId, username, password");
        }
        
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
        
        if (Integer.valueOf(0).equals(req.getMerchantId())) {
            // Find user by username (admin or merchant)
            user = users.findByUsername(req.getUsername())
                .filter(u -> "merchant".equals(u.getUserType()) || "super_admin".equals(u.getUserType()))
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password. Please check your credentials."));
            
            // Check if user is active
            if (!Boolean.TRUE.equals(user.getActive())) {
                throw new AccessDeniedException("Account is inactive. Please contact support.");
            }
        }
        else if (req.getMerchantId() > 0) {
            user = users.findByPhoneAndMerchant_MerchantId(req.getUsername(), req.getMerchantId())
                    .orElseThrow(() -> new UserNotFoundException("Customer not found for this merchant. Please check your phone number."));
        }
        else {
            throw new IllegalArgumentException("Invalid merchantId. Use 0 for admin/merchant, >0 for customers");
        }
        
        // Verify password
        if (user.getPasswordHash() == null || user.getPasswordHash().trim().isEmpty()) {
            throw new ValidationException("Account setup incomplete. Please contact support.");
        }
        
        if (!encoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid password. Please check your credentials.");
        }
        
        log.info("Login successful for user: {} (type: {})", user.getUserId(), user.getUserType());
        return buildTokens(user, req.getMerchantId());
    }
    
    private User findUserByPhoneAndMerchantId(String phone, Integer merchantId) {
        if (merchantId == null || merchantId == 0) {
            return users.findByPhoneAndMerchantIsNull(phone)
                    .or(() -> users.findByPhone(phone).stream().findFirst())
                    .orElseThrow(() -> new UserNotFoundException("No user found with phone: " + maskPhone(phone)));
        } else {
            return users.findByPhoneAndMerchant_MerchantId(phone, merchantId)
                    .orElseThrow(() -> new UserNotFoundException("No customer found with phone: " + maskPhone(phone) + " for merchant: " + merchantId));
        }
    }

    @Override
    public AuthResponse refresh(RefreshTokenRequest req) {
        try {
            if (jwt.isTokenBlacklisted(req.getRefreshToken())) {
                throw new InvalidCredentialsException("Token has been revoked. Please login again.");
            }
            Claims claims = jwt.parse(req.getRefreshToken());
            Integer userId = Integer.valueOf(claims.getSubject());
            Integer merchantId = (Integer) claims.get("merchantId");
            User user = users.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found for token refresh."));
            
            jwt.blacklistToken(req.getRefreshToken());
            
            return buildTokens(user, merchantId);
        } catch (io.jsonwebtoken.JwtException e) {
            log.warn("JWT exception during token refresh: {}", e.getMessage());
            throw new InvalidCredentialsException("Invalid refresh token. Please login again.");
        } catch (IllegalArgumentException e) {
            log.warn("Invalid argument during token refresh: {}", e.getMessage());
            throw new InvalidCredentialsException("Invalid refresh token. Please login again.");
        }
    }

    private AuthResponse buildTokens(User user, Integer merchantId) {
        // For merchant/admin users (merchantId=0 in request), return their actual merchantId
        Integer actualMerchantId;
        if (merchantId != null && Integer.valueOf(0).equals(merchantId)) {
            // Merchant/admin login - return their actual merchantId from user entity, or 0 if no merchant
            actualMerchantId = user.getMerchant() != null ? user.getMerchant().getMerchantId() : 0;
        } else {
            // Customer login - use provided merchantId or user's merchantId
            actualMerchantId = merchantId != null ? merchantId :
                    (user.getMerchant() != null ? user.getMerchant().getMerchantId() : null);
        }
        
        // For superadmin users, use null instead of 0 for merchant queries
        Integer queryMerchantId = (actualMerchantId != null && Integer.valueOf(0).equals(actualMerchantId)) ? null : actualMerchantId;
        
        // Cache user roles and permissions lookup
        List<String> roleNames;
        List<String> permissionNames;
        try {
            roleNames = userRoles.findRoleNames(user.getUserId(), queryMerchantId);
            permissionNames = userRoles.findPermissionNames(user.getUserId(), queryMerchantId);
        } catch (Exception e) {
            log.warn("Error fetching roles/permissions for user {}: {}", user.getUserId(), e.getMessage());
            roleNames = List.of(getDefaultRoleForUserType(user.getUserType()));
            permissionNames = List.of();
        }
        
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
        
        // Handle leading zeros by comparing as integers
        try {
            int inputInt = Integer.parseInt(inputOtp);
            int storedInt = Integer.parseInt(storedOtp);
            return inputInt == storedInt;
        } catch (NumberFormatException e) {
            // Fallback to string comparison
            return otpService.verifyOtp(inputOtp, storedOtp);
        }
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
        try {
            return userRoles.existsByUser_UserIdAndMerchant_MerchantId(userId, merchantId);
        } catch (Exception e) {
            log.warn("Error checking user-merchant association for userId: {}, merchantId: {}", userId, merchantId);
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
            log.warn("Error checking phone block status for merchantId: {}", merchantId);
            return false;
        }
    }
    
    private void validateOtpRateLimit(String phone, String otpType) {
        LocalDateTime timeLimit = LocalDateTime.now().minusMinutes(AppConstants.OTP_RATE_LIMIT_WINDOW_MINUTES);
        int recentRequests = otpLogRepository.countByPhoneAndCreatedOnAfter(phone, timeLimit);
        
        if (recentRequests >= AppConstants.OTP_RATE_LIMIT_REQUESTS) {
            log.warn("Rate limit exceeded for phone: {}, type: {}, requests: {} in last {} minutes", 
                    maskPhone(phone), otpType, recentRequests, AppConstants.OTP_RATE_LIMIT_WINDOW_MINUTES);
            
            String errorMessage = String.format(
                "Too many OTP requests. You have exceeded the limit of %d OTP requests. Please try again after %d minutes.", 
                AppConstants.OTP_RATE_LIMIT_REQUESTS, 
                AppConstants.OTP_RATE_LIMIT_WINDOW_MINUTES
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
        return switch (otpType) {
            case "password_reset", "phone_verification", "account_verification" -> LocalDateTime.now().plusMinutes(AppConstants.OTP_EXPIRY_MINUTES);
            default -> LocalDateTime.now().plusMinutes(AppConstants.OTP_EXPIRY_MINUTES);
        };
    }
    
    private boolean sendOtpByType(String phone, String otpCode) {
        return smsService.sendOtp(phone, otpCode);
    }
    
    private void clearOtpData(User user) {
        user.setOtpCode(null);
        user.setOtpExpiresAt(null);
        user.setOtpAttempts(0);
        user.setOtpUsed(false);
        users.save(user);
    }
    
    private String generateRandomPassword() {
        final String upperCase = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        final String lowerCase = "abcdefghijklmnopqrstuvwxyz";
        final String digits = "0123456789";
        final String specialChars = "@$!%*?&";
        final String allChars = upperCase + lowerCase + digits + specialChars;
        
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(10);
        
        // Ensure at least one character from each category
        password.append(upperCase.charAt(random.nextInt(upperCase.length())));
        password.append(lowerCase.charAt(random.nextInt(lowerCase.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        password.append(specialChars.charAt(random.nextInt(specialChars.length())));
        
        // Fill remaining positions with random characters
        for (int i = 4; i < 10; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }
        
        // Shuffle the password to randomize character positions
        return shuffleString(password.toString(), random);
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
    
    @Override
    @Transactional
    public void logout(Integer userId) {
        try {
            org.springframework.security.core.Authentication auth = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getCredentials() instanceof String) {
                String token = (String) auth.getCredentials();
                jwt.blacklistToken(token);
                log.debug("Token blacklisted for user: {}", userId);
            }
            
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
            
            log.info("User {} logged out successfully with token invalidation", userId);
        } catch (Exception e) {
            log.warn("Error during logout for user {}: {}", userId, e.getMessage());
            throw new ServiceUnavailableException("Logout failed. Please try again.");
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
            
            if (req.getOtpType() != null && !req.getOtpType().matches("^(login|password_reset|registration|phone_verification)$")) {
                throw new IllegalArgumentException("Invalid OTP type. Must be one of: login, password_reset, registration, phone_verification");
            }
            
            User user;
            try {
                user = findUserByPhoneAndMerchantId(req.getPhone(), req.getMerchantId());
                log.debug("Found user for OTP request: userId={}, userType={}, merchantId={}", 
                         user.getUserId(), user.getUserType(), 
                         user.getMerchant() != null ? user.getMerchant().getMerchantId() : "null");
            } catch (RuntimeException e) {
                log.warn("User not found for OTP request with merchantId: {}", req.getMerchantId());
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
                log.warn("Role validation failed for userId: {}, merchantId: {}", user.getUserId(), req.getMerchantId());
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
                log.warn("Error checking phone block status, continuing with OTP request");
            }
            
            try {
                validateOtpRateLimit(req.getPhone(), otpType);
            } catch (RuntimeException e) {
                log.warn("Rate limit validation failed for otpType: {}", otpType);
                throw e;
            }
            
            try {
                invalidateExistingOtp(user);
            } catch (Exception e) {
                log.warn("Failed to invalidate existing OTP, continuing with request");
            }
            
            String otpCode = generateOtp();
            LocalDateTime expiresAt = getExpiryByType(otpType);
            log.debug("Generated OTP for phone: {}, expires at: {}", maskedPhone, expiresAt);
            
            user.setOtpCode(otpCode);
            user.setOtpExpiresAt(expiresAt);
            user.setOtpAttempts(0);
            user.setOtpUsed(false);
            users.save(user);
            log.debug("OTP stored successfully for user: {}", user.getUserId());
            
            boolean smsSent = sendOtpByType(req.getPhone(), otpCode);
            String status = smsSent ? "sent" : "send_failed";
            log.debug("SMS send result for phone: {}, status: {}", maskedPhone, status);
            
            try {
                Integer merchantId = user.getMerchant() != null ? user.getMerchant().getMerchantId() : req.getMerchantId();
                otpAuditService.logOtp(merchantId, req.getPhone(), otpCode, otpType, status, expiresAt);
                log.debug("OTP audit log created for phone: {}", maskedPhone);
            } catch (Exception e) {
                log.warn("Failed to create OTP audit log");
            }
            
            if (!smsSent) {
                throw new RuntimeException("SMS service temporarily unavailable. Please try again.");
            }
            
            log.info("OTP sent successfully to phone: {}, type: {}", maskedPhone, otpType);
            
        } catch (RuntimeException e) {
            log.warn("OTP request failed for otpType: {}", otpType);
            throw e;
        } catch (Exception e) {
            log.warn("OTP request processing failed for otpType: {}", otpType);
            throw new RuntimeException("Failed to process OTP request", e);
        }
    }
    
    @Override
    public String verifyOtpWithStatus(OtpVerifyRequest req) {
        String maskedPhone = maskPhone(req.getPhone());
        log.info("OTP verification status check for phone: {}, otpType: {}", maskedPhone, req.getOtpType());

        try {
            User user = findUserByPhoneAndMerchantId(req.getPhone(), req.getMerchantId());


            
            if (user.getOtpCode() == null) {
                log.warn("No active OTP found for phone: {}", maskedPhone);
                return "NO_OTP_REQUEST";
            }
            
            if (Boolean.TRUE.equals(user.getOtpUsed())) {
                log.warn("OTP already used for phone: {}", maskedPhone);
                return "ALREADY_USED";
            }
            
            if (user.getOtpExpiresAt() == null || user.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
                log.warn("Expired OTP verification attempt for phone: {}", maskedPhone);
                return "EXPIRED";
            }
            
            Integer otpAttempts = user.getOtpAttempts();
            int currentAttempts = otpAttempts != null ? otpAttempts : 0;
            if (currentAttempts >= AppConstants.OTP_MAX_ATTEMPTS) {
                log.warn("Max OTP attempts exceeded for phone: {}", maskedPhone);
                return "INVALID";
            }
            
            // Check for leading zeros handling
            String inputOtp = req.getOtp();
            String storedOtp = user.getOtpCode();
            
            if (!isOtpValid(inputOtp, storedOtp)) {
                log.warn("Invalid OTP attempt {} for phone: {}", currentAttempts + 1, maskedPhone);
                return "INVALID";
            }
            
            log.info("OTP verification status: SUCCESS for phone: {}", maskedPhone);
            return "SUCCESS";
            
        } catch (RuntimeException e) {
            log.warn("OTP verification status check failed");
            return "INVALID";
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
            
            if (user.getOtpExpiresAt() == null || user.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
                otpAuditService.updateOtpExpired(req.getPhone());
                clearOtpData(user);
                log.warn("Expired OTP verification attempt for phone: {}", maskedPhone);
                return null;
            }
            
            Integer otpAttempts = user.getOtpAttempts();
            int currentAttempts = otpAttempts != null ? otpAttempts : 0;
            if (currentAttempts >= AppConstants.OTP_MAX_ATTEMPTS) {
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
            
            Integer merchantId = user.getMerchant() != null ? user.getMerchant().getMerchantId() : req.getMerchantId();
            otpAuditService.logOtpVerified(req.getPhone(), merchantId);
            
            // Mark OTP as used before clearing
            user.setOtpUsed(true);
            users.save(user);
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
            log.warn("OTP verification failed");
            return null;
        }
    }
    
    @Override
    public boolean isPhoneNumberExists(String phone, Integer merchantId) {
        try {
            findUserByPhoneAndMerchantId(phone, merchantId);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
    
    @Override
    public long getUserCount() {
        return users.count();
    }
}