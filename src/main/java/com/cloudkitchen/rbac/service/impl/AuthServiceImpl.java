package com.cloudkitchen.rbac.service.impl;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.cloudkitchen.rbac.config.SecurityProperties;
import com.cloudkitchen.rbac.constants.AppConstants;
import com.cloudkitchen.rbac.domain.entity.Customer;
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
import com.cloudkitchen.rbac.exception.BusinessExceptions.AccessDeniedException;
import com.cloudkitchen.rbac.exception.BusinessExceptions.InvalidCredentialsException;
import com.cloudkitchen.rbac.exception.BusinessExceptions.InvalidOtpException;
import com.cloudkitchen.rbac.exception.BusinessExceptions.MerchantNotFoundException;
import com.cloudkitchen.rbac.exception.BusinessExceptions.MobileNotRegisteredException;
import com.cloudkitchen.rbac.exception.BusinessExceptions.OtpAttemptsExceededException;
import com.cloudkitchen.rbac.exception.BusinessExceptions.OtpExpiredException;
import com.cloudkitchen.rbac.exception.BusinessExceptions.OtpNotFoundException;
import com.cloudkitchen.rbac.exception.BusinessExceptions.ServiceUnavailableException;
import com.cloudkitchen.rbac.exception.BusinessExceptions.UserAlreadyExistsException;
import com.cloudkitchen.rbac.exception.BusinessExceptions.UserNotFoundException;
import com.cloudkitchen.rbac.exception.BusinessExceptions.ValidationException;
import com.cloudkitchen.rbac.repository.CustomerRepository;
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
    private static final String ROLE_CUSTOMER = "customer";
    private static final String ROLE_MERCHANT = "merchant";
    private static final String ROLE_SUPER_ADMIN = "super_admin";
    private static final String OTP_TYPE_PASSWORD_RESET = "password_reset";
    private static final String INVALID_OTP_ATTEMPT_LOG = "Invalid OTP attempt {} for phone: {}";

    private final UserRepository users;
    private final MerchantRepository merchants;
    private final RoleRepository roles;
    private final UserRoleRepository userRoles;
    private final CustomerRepository customers;
    private final PasswordEncoder encoder;
    private final OtpService otpService;
    private final OtpAuditService otpAuditService;
    private final OtpLogRepository otpLogRepository;
    private final SmsService smsService;
    private final JwtTokenProvider jwt;
    private final ValidationService validationService;
    private final SecurityProperties securityProperties;

    public AuthServiceImpl(UserRepository users, MerchantRepository merchants, RoleRepository roles,
            UserRoleRepository userRoles, CustomerRepository customers, PasswordEncoder encoder, OtpService otpService,
            OtpAuditService otpAuditService, SmsService smsService, JwtTokenProvider jwt,
            OtpLogRepository otpLogRepository, ValidationService validationService,
            SecurityProperties securityProperties) {
        this.users = users;
        this.merchants = merchants;
        this.roles = roles;
        this.userRoles = userRoles;
        this.customers = customers;
        this.encoder = encoder;
        this.otpService = otpService;
        this.otpAuditService = otpAuditService;
        this.smsService = smsService;
        this.jwt = jwt;
        this.otpLogRepository = otpLogRepository;
        this.validationService = validationService;
        this.securityProperties = securityProperties;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4)
            return "****";
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
            throw new UserAlreadyExistsException("Phone number " + maskPhone(req.getPhone())
                    + " is already registered for this merchant. Please use a different phone number or login instead.");
        }

        Merchant merchant = merchants.findById(req.getMerchantId())
                .orElseThrow(() -> new MerchantNotFoundException(
                        "Merchant with ID " + req.getMerchantId() + " not found. Please contact support."));

        User user = createUser(req, merchant, ROLE_CUSTOMER);
        user = users.save(user);

        assignUserRole(user, ROLE_CUSTOMER, merchant);
        
        Customer customer = createCustomer(req, merchant, user);

        return buildTokens(user, merchant.getMerchantId(), customer.getCustomerId());
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
            case ROLE_SUPER_ADMIN -> ROLE_SUPER_ADMIN;
            case ROLE_MERCHANT -> ROLE_MERCHANT;
            default -> ROLE_CUSTOMER;
        };

        Role role = roles.findByRoleName(roleName)
                .orElseThrow(() -> new IllegalStateException(
                        roleName + " role not found. Please ensure roles are properly initialized."));

        UserRole userRole = new UserRole();
        userRole.setUser(user);
        userRole.setRole(role);
        userRole.setMerchant(merchant);
        userRole.setAssignedAt(LocalDateTime.now());
        userRoles.save(userRole);
    }

    private Customer createCustomer(RegisterRequest req, Merchant merchant, User user) {
        Customer customer = new Customer();
        customer.setMerchant(merchant);
        customer.setUser(user);
        customer.setPhone(req.getPhone());
        customer.setFirstName(req.getFirstName());
        customer.setLastName(req.getLastName());
        customer.setAddress(req.getAddress());
        customer.setIsActive(true);
        customer.setCreatedBy(user.getUserId());
        customer.setCreatedOn(LocalDateTime.now());
        customer.setUpdatedOn(LocalDateTime.now());
        customer = customers.save(customer);
        log.info("Customer record created: customerId={}, userId={}, merchantId={}", 
                customer.getCustomerId(), user.getUserId(), merchant.getMerchantId());
        return customer;
    }

    @Override
    public AuthResponse login(AuthRequest req) {
        log.info("Login attempt for merchantId: {}", req.getMerchantId());
        
        // 1. INPUT VALIDATION FIRST (before any business logic)
        validateLoginInputs(req);
        
        // 2. BUSINESS VALIDATION
        User user = findUserForLogin(req);
        verifyPassword(user, req.getPassword());
        Integer customerId = getCustomerId(user, req.getMerchantId());
        
        log.info("Login successful for user: {} (type: {})", user.getUserId(), user.getUserType());
        return buildTokens(user, req.getMerchantId(), customerId);
    }
    
    private void validateLoginInputs(AuthRequest req) {
        // Validate merchantId first
        if (req.getMerchantId() == null) {
            throw new ValidationException("MerchantId is required: use 0 for admin/merchant, >0 for customers");
        }
        
        // Validate username/mobile
        if (req.getUsername() == null || req.getUsername().trim().isEmpty()) {
            throw new ValidationException("Username is required");
        }
        
        // For customer login (merchantId > 0), validate mobile format
        if (req.getMerchantId() > 0) {
            try {
                validationService.validateMobileForLogin(req.getUsername());
            } catch (IllegalArgumentException e) {
                throw new ValidationException(e.getMessage());
            }
        }
        
        // Validate password
        if (req.getPassword() == null || req.getPassword().trim().isEmpty()) {
            throw new ValidationException("Password is required");
        }
    }

    private User findUserForLogin(AuthRequest req) {
        if (Integer.valueOf(0).equals(req.getMerchantId())) {
            return findMerchantOrAdminUser(req.getUsername());
        } else if (req.getMerchantId() > 0) {
            return users.findByPhoneAndMerchant_MerchantId(req.getUsername(), req.getMerchantId())
                    .orElseThrow(() -> new MobileNotRegisteredException(
                            "Mobile number not registered"));
        } else {
            throw new ValidationException("Invalid merchantId. Use 0 for admin/merchant, >0 for customers");
        }
    }

    private User findMerchantOrAdminUser(String username) {
        User user = users.findByUsername(username)
                .filter(u -> ROLE_MERCHANT.equals(u.getUserType()) || ROLE_SUPER_ADMIN.equals(u.getUserType()))
                .orElseThrow(() -> new InvalidCredentialsException(
                        "Invalid username or password. Please check your credentials."));

        if (!Boolean.TRUE.equals(user.getActive())) {
            throw new AccessDeniedException("Account is inactive. Please contact support.");
        }
        return user;
    }

    private void verifyPassword(User user, String password) {
        if (user.getPasswordHash() == null || user.getPasswordHash().trim().isEmpty()) {
            throw new ValidationException("Account setup incomplete. Please contact support.");
        }
        if (!encoder.matches(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid password. Please check your credentials.");
        }
    }

    private User findUserByPhoneAndMerchantId(String phone, Integer merchantId) {
        if (merchantId == null || merchantId == 0) {
            return users.findByPhoneAndMerchantIsNull(phone)
                    .or(() -> users.findByPhone(phone).stream().findFirst())
                    .orElseThrow(() -> new UserNotFoundException("No user found with phone: " + maskPhone(phone)));
        } else {
            return users.findByPhoneAndMerchant_MerchantId(phone, merchantId)
                    .orElseThrow(() -> new UserNotFoundException(
                            "No customer found with phone: " + maskPhone(phone) + " for merchant: " + merchantId));
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
            User user = users.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException("User not found for token refresh."));

            jwt.blacklistToken(req.getRefreshToken());
            
            Integer customerId = getCustomerId(user, merchantId);
            return buildTokens(user, merchantId, customerId);
        } catch (io.jsonwebtoken.JwtException e) {
            log.warn("JWT exception during token refresh: {}", e.getMessage());
            throw new InvalidCredentialsException("Invalid refresh token. Please login again.");
        } catch (IllegalArgumentException e) {
            log.warn("Invalid argument during token refresh: {}", e.getMessage());
            throw new InvalidCredentialsException("Invalid refresh token. Please login again.");
        }
    }

    private Integer getCustomerId(User user, Integer merchantId) {
        if (ROLE_CUSTOMER.equals(user.getUserType()) && merchantId != null && merchantId > 0) {
            return customers.findByUser_UserIdAndMerchant_MerchantId(user.getUserId(), merchantId)
                    .map(Customer::getCustomerId)
                    .orElse(null);
        }
        return null;
    }

    private AuthResponse buildTokens(User user, Integer merchantId, Integer customerId) {
        Integer actualMerchantId = determineActualMerchantId(user, merchantId);
        Integer queryMerchantId = (actualMerchantId != null && Integer.valueOf(0).equals(actualMerchantId)) ? null
                : actualMerchantId;

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
        response.setCustomerId(customerId);
        response.setPhone(user.getPhone());
        response.setRoles(roleNames);
        return response;
    }

    /**
     * Determine the effective merchant ID based on the supplied merchantId and the user's associated merchant.
     * @param user - The user whose associated merchant may be used as a fallback.
     * @param merchantId - The requested merchant ID; 0 means use the user's merchant ID if present else 0, null means use the user's merchant ID if present else null.
     * @return The resolved merchant ID: returns merchantId when non-null (0 handled as above), otherwise returns the user's merchant ID or the appropriate fallback (0 or null).
     */
    private Integer determineActualMerchantId(User user, Integer merchantId) {
        if (merchantId != null) {
            if (merchantId == 0) {
                return user.getMerchant() != null ? user.getMerchant().getMerchantId() : 0;
            }
            return merchantId;
        }
        return user.getMerchant() != null ? user.getMerchant().getMerchantId() : null;
    }
    private boolean isOtpValid(String inputOtp, String storedOtp) {
        if (inputOtp == null || storedOtp == null)
            return false;

        // Constant-time comparison to prevent timing attacks
        try {
            // Normalize both OTPs to same format
            int inputNum = Integer.parseInt(inputOtp);
            int storedNum = Integer.parseInt(storedOtp);
            String normalizedInput = String.format("%06d", inputNum);
            String normalizedStored = String.format("%06d", storedNum);

            // Use constant-time comparison
            return java.security.MessageDigest.isEqual(
                    normalizedInput.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    normalizedStored.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (NumberFormatException e) {
            // Fallback to constant-time string comparison
            return java.security.MessageDigest.isEqual(
                    inputOtp.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    storedOtp.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }

    private void validateUserRoleForOtp(User user, Integer merchantId) {
        if (merchantId == null || merchantId == 0) {
            if (log.isDebugEnabled()) {
                log.debug("OTP request with merchantId=0 for phone: {}, allowing OTP generation",
                        maskPhone(user.getPhone()));
            }
        } else {
            if (!ROLE_CUSTOMER.equals(user.getUserType())) {
                throw new AccessDeniedException("Access denied. Only customers can request OTP for specific merchant.");
            }

            if (!isUserAssociatedWithMerchant(user.getUserId(), merchantId)) {
                throw new AccessDeniedException("Access denied. User is not associated with the given merchant.");
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

            if (isBlocked && log.isWarnEnabled()) {
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
            if (log.isWarnEnabled()) {
                log.warn("Rate limit exceeded for phone: {}, type: {}, requests: {} in last {} minutes",
                        maskPhone(phone), otpType, recentRequests, AppConstants.OTP_RATE_LIMIT_WINDOW_MINUTES);
            }

            String errorMessage = String.format(
                    "Too many OTP requests. You have exceeded the limit of %d OTP requests. Please try again after %d minutes.",
                    AppConstants.OTP_RATE_LIMIT_REQUESTS,
                    AppConstants.OTP_RATE_LIMIT_WINDOW_MINUTES);
            throw new ValidationException(errorMessage);
        }
    }

    private void invalidateExistingOtp(User user) {
        if (user.getOtpCode() != null) {
            clearOtpData(user);
            if (log.isDebugEnabled()) {
                log.debug("Cleared existing OTP for user: {}", user.getUserId());
            }
        }
    }

    private String generateOtp() {
        return otpService.generateOtp();
    }

    private LocalDateTime getExpiryByType() {
        return LocalDateTime.now().plusMinutes(AppConstants.OTP_EXPIRY_MINUTES);
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
            case ROLE_SUPER_ADMIN -> ROLE_SUPER_ADMIN;
            case ROLE_MERCHANT -> ROLE_MERCHANT;
            case ROLE_CUSTOMER -> ROLE_CUSTOMER;
            default -> ROLE_CUSTOMER;
        };
    }

    @Override
    @Transactional
    public void logout(Integer userId) {
        try {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication();
            if (auth != null && auth.getCredentials() instanceof String token) {
                jwt.blacklistToken(token);
                if (log.isDebugEnabled()) {
                    log.debug("Token blacklisted for user: {}", userId);
                }
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

            String cleanPhone = req.getPhone().trim().replaceAll("\\D", "");
            validationService.validatePhone(cleanPhone);

            if (req.getOtpType() != null
                    && !req.getOtpType().matches("^(login|password_reset|registration|phone_verification)$")) {
                throw new IllegalArgumentException(
                        "Invalid OTP type. Must be one of: login, password_reset, registration, phone_verification");
            }

            User user = findUserForOtpRequest(req, otpType);
            validateUserRoleForOtpRequest(user, req.getMerchantId());
            checkPhoneBlockStatus(req.getPhone(), req.getMerchantId(), maskedPhone);
            validateOtpRateLimitForRequest(req.getPhone(), otpType);
            invalidateExistingOtpSafely(user);

            String otpCode = generateOtp();
            LocalDateTime expiresAt = getExpiryByType();
            if (log.isDebugEnabled()) {
                log.debug("Generated OTP for phone: {}, expires at: {}", maskedPhone, expiresAt);
            }

            // SECURITY: Environment-aware OTP storage
            // DEVELOPMENT MODE: Store plaintext OTP (for easier testing/debugging)
            // PRODUCTION MODE: Should hash OTP before storage (future enhancement)
            if (securityProperties.isHashOtp()) {
                // TODO: Production mode - hash OTP before storage
                // user.setOtpCode(encoder.encode(otpCode));
                log.warn("OTP hashing is enabled but not yet implemented. Storing plaintext for now.");
                user.setOtpCode(otpCode);
            } else {
                // Development mode - store plaintext
                if (securityProperties.isDevelopmentMode()) {
                    log.debug("DEVELOPMENT MODE: Storing OTP in plaintext for phone: {}", maskedPhone);
                }
                user.setOtpCode(otpCode);
            }

            user.setOtpExpiresAt(expiresAt);
            user.setOtpAttempts(0);
            user.setOtpUsed(false);
            users.save(user);

            if (log.isDebugEnabled()) {
                log.debug("OTP stored successfully for user: {} (hashOtp={})",
                    user.getUserId(), securityProperties.isHashOtp());
            }

            boolean smsSent = sendOtpByType(req.getPhone(), otpCode);
            String status = smsSent ? "sent" : "send_failed";

            if (log.isDebugEnabled()) {
                log.debug("SMS send result for phone: {}, status: {}", maskedPhone, status);
            }

            logOtpAudit(user, req, otpCode, otpType, status, expiresAt, maskedPhone);

            if (!smsSent) {
                throw new ServiceUnavailableException("SMS service temporarily unavailable. Please try again.");
            }

            log.info("OTP sent successfully to phone: {}, type: {}", maskedPhone, otpType);

        } catch (RuntimeException e) {
            log.warn("OTP request failed for otpType: {}", otpType);
            throw e;
        }
    }

    private User findUserForOtpRequest(OtpRequest req, String otpType) {
        try {
            User user = findUserByPhoneAndMerchantId(req.getPhone(), req.getMerchantId());
            if (log.isDebugEnabled()) {
                log.debug("Found user for OTP request: userId={}, userType={}, merchantId={}",
                        user.getUserId(), user.getUserType(),
                        user.getMerchant() != null ? user.getMerchant().getMerchantId() : "null");
            }
            return user;
        } catch (RuntimeException e) {
            log.warn("User not found for OTP request with merchantId: {}", req.getMerchantId());
            if (OTP_TYPE_PASSWORD_RESET.equals(otpType)) {
                throw new UserNotFoundException(
                        "User not found. Please register first or check your phone number and merchant ID.");
            }
            throw new UserNotFoundException(
                    "User not found for the provided phone number and merchant. Please register first.");
        }
    }

    private void validateUserRoleForOtpRequest(User user, Integer merchantId) {
        try {
            validateUserRoleForOtp(user, merchantId);
            if (log.isDebugEnabled()) {
                log.debug("User role validation passed for userId={}, userType={}, merchantId={}",
                        user.getUserId(), user.getUserType(), merchantId);
            }
        } catch (RuntimeException e) {
            log.warn("Role validation failed for userId: {}, merchantId: {}", user.getUserId(), merchantId);
            throw e;
        }
    }

    private void checkPhoneBlockStatus(String phone, Integer merchantId, String maskedPhone) {
        try {
            if (isPhoneBlocked(phone, merchantId)) {
                log.warn("Phone blocked for OTP requests: phone={}, merchantId={}", maskedPhone, merchantId);
                throw new AccessDeniedException(
                        "Phone is blocked due to too many failed OTP attempts. Please try again later.");
            }
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Error checking phone block status, continuing with OTP request");
        }
    }

    private void validateOtpRateLimitForRequest(String phone, String otpType) {
        try {
            validateOtpRateLimit(phone, otpType);
        } catch (RuntimeException e) {
            log.warn("Rate limit validation failed for otpType: {}", otpType);
            throw e;
        }
    }

    private void invalidateExistingOtpSafely(User user) {
        try {
            invalidateExistingOtp(user);
        } catch (Exception e) {
            log.warn("Failed to invalidate existing OTP, continuing with request");
        }
    }

    @Override
    @Transactional
    public String verifyOtpWithStatus(OtpVerifyRequest req) {
        String maskedPhone = maskPhone(req.getPhone());
        log.info("OTP verification for phone: {}, otpType: {}", maskedPhone, req.getOtpType());

        // 1. INPUT VALIDATION FIRST (before any business logic)
        try {
            validationService.validateMobileForOtp(req.getPhone());
        } catch (IllegalArgumentException e) {
            String errorMsg = "Mobile validation failed for phone " + maskedPhone + ": " + e.getMessage();
            log.warn(errorMsg);
            throw new ValidationException(e.getMessage(), e);
        }

        try {
            validationService.validateOtp(req.getOtp());
        } catch (IllegalArgumentException e) {
            String errorMsg = "OTP format validation failed for phone " + maskedPhone + ": " + e.getMessage();
            log.warn(errorMsg);
            throw new ValidationException(e.getMessage(), e);
        }

        // 2. CHECK IF MOBILE IS REGISTERED
        User user;
        try {
            user = findUserByPhoneAndMerchantId(req.getPhone(), req.getMerchantId());
        } catch (UserNotFoundException e) {
            log.warn("Mobile number not registered: {}", maskedPhone);
            throw new MobileNotRegisteredException("Mobile number not registered");
        }

        // 3. CHECK IF OTP REQUEST EXISTS
        if (user.getOtpCode() == null) {
            log.warn("No OTP request found for phone: {}", maskedPhone);
            throw new OtpNotFoundException("OTP not requested");
        }

        // 4. CHECK IF OTP IS EXPIRED (before checking attempts or validity)
        if (user.getOtpExpiresAt() == null || user.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("OTP expired for phone: {}", maskedPhone);
            otpAuditService.updateOtpExpired(req.getPhone());
            clearOtpData(user);
            throw new OtpExpiredException("OTP expired");
        }

        // 5. CHECK ATTEMPT LIMIT
        Integer otpAttempts = user.getOtpAttempts();
        int currentAttempts = otpAttempts != null ? otpAttempts : 0;
        if (currentAttempts >= AppConstants.OTP_MAX_ATTEMPTS) {
            log.warn("OTP attempts exceeded for phone: {}", maskedPhone);
            otpAuditService.updateOtpFailed(req.getPhone(), currentAttempts);
            clearOtpData(user);
            throw new OtpAttemptsExceededException("OTP verification attempts exceeded");
        }

        // 6. VERIFY OTP
        if (!isOtpValid(req.getOtp(), user.getOtpCode())) {
            // Increment attempt counter
            user.setOtpAttempts(currentAttempts + 1);
            users.save(user);
            otpAuditService.updateOtpFailed(req.getPhone(), currentAttempts + 1);
            log.warn(INVALID_OTP_ATTEMPT_LOG, currentAttempts + 1, maskedPhone);
            throw new InvalidOtpException("Invalid OTP");
        }

        // 7. SUCCESS - Mark as used but don't clear yet (verifyOtp will handle that)
        log.info("OTP verification successful for phone: {}", maskedPhone);
        return "SUCCESS";
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
                log.warn(INVALID_OTP_ATTEMPT_LOG, currentAttempts + 1, maskedPhone);
                return null;
            }

            Integer merchantId = user.getMerchant() != null ? user.getMerchant().getMerchantId() : req.getMerchantId();
            otpAuditService.logOtpVerified(req.getPhone(), merchantId);

            // Mark OTP as used before clearing
            user.setOtpUsed(true);
            users.save(user);
            clearOtpData(user);

            Integer customerId = getCustomerId(user, merchantId);
            
            if (OTP_TYPE_PASSWORD_RESET.equals(req.getOtpType())) {
                String randomPassword = generateRandomPassword();
                user.setPasswordHash(encoder.encode(randomPassword));
                users.save(user);
                log.info("Password reset with random password for user: {}", maskedPhone);
                return buildTokens(user, req.getMerchantId(), customerId);
            }

            log.info("OTP verified successfully for phone: {}", maskedPhone);
            return buildTokens(user, req.getMerchantId(), customerId);

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

    private void logOtpAudit(User user, OtpRequest req, String otpCode, String otpType, String status,
            LocalDateTime expiresAt, String maskedPhone) {
        try {
            Integer merchantId = user.getMerchant() != null ? user.getMerchant().getMerchantId() : req.getMerchantId();
            otpAuditService.logOtp(merchantId, req.getPhone(), otpCode, otpType, status, expiresAt);
            if (log.isDebugEnabled()) {
                log.debug("OTP audit log created for phone: {}", maskedPhone);
            }
        } catch (Exception e) {
            log.warn("Failed to create OTP audit log");
        }
    }

    @Override
    @Transactional
    public AuthResponse verifyOtpAndGenerateToken(OtpVerifyRequest req) {
        String maskedPhone = maskPhone(req.getPhone());
        log.info("OTP verification and token generation for phone: {}, otpType: {}", maskedPhone, req.getOtpType());

        // 1. INPUT VALIDATION FIRST (before any business logic)
        try {
            validationService.validateMobileForOtp(req.getPhone());
        } catch (IllegalArgumentException e) {
            String errorMsg = "Mobile validation failed for phone " + maskedPhone + " during token generation: " + e.getMessage();
            log.warn(errorMsg);
            throw new ValidationException(e.getMessage(), e);
        }

        try {
            validationService.validateOtp(req.getOtp());
        } catch (IllegalArgumentException e) {
            String errorMsg = "OTP format validation failed for phone " + maskedPhone + " during token generation: " + e.getMessage();
            log.warn(errorMsg);
            throw new ValidationException(e.getMessage(), e);
        }

        // 2. CHECK IF MOBILE IS REGISTERED
        User user;
        try {
            user = findUserByPhoneAndMerchantId(req.getPhone(), req.getMerchantId());
        } catch (UserNotFoundException e) {
            log.warn("Mobile number not registered: {}", maskedPhone);
            throw new MobileNotRegisteredException("Mobile number not registered");
        }

        // 3. CHECK IF OTP REQUEST EXISTS
        if (user.getOtpCode() == null) {
            log.warn("No OTP request found for phone: {}", maskedPhone);
            throw new OtpNotFoundException("OTP not requested");
        }

        // 4. CHECK IF OTP IS EXPIRED (before checking attempts or validity)
        if (user.getOtpExpiresAt() == null || user.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("OTP expired for phone: {}", maskedPhone);
            otpAuditService.updateOtpExpired(req.getPhone());
            clearOtpData(user);
            throw new OtpExpiredException("OTP expired");
        }

        // 5. CHECK ATTEMPT LIMIT
        Integer otpAttempts = user.getOtpAttempts();
        int currentAttempts = otpAttempts != null ? otpAttempts : 0;
        if (currentAttempts >= AppConstants.OTP_MAX_ATTEMPTS) {
            log.warn("OTP attempts exceeded for phone: {}", maskedPhone);
            otpAuditService.updateOtpFailed(req.getPhone(), currentAttempts);
            clearOtpData(user);
            throw new OtpAttemptsExceededException("OTP verification attempts exceeded");
        }

        // 6. VERIFY OTP
        if (!isOtpValid(req.getOtp(), user.getOtpCode())) {
            // Increment attempt counter
            user.setOtpAttempts(currentAttempts + 1);
            users.save(user);
            otpAuditService.updateOtpFailed(req.getPhone(), currentAttempts + 1);
            log.warn(INVALID_OTP_ATTEMPT_LOG, currentAttempts + 1, maskedPhone);
            throw new InvalidOtpException("Invalid OTP");
        }

        // 7. SUCCESS - Log audit and generate tokens
        Integer merchantId = user.getMerchant() != null ? user.getMerchant().getMerchantId() : req.getMerchantId();
        otpAuditService.logOtpVerified(req.getPhone(), merchantId);

        // Mark OTP as used and clear
        user.setOtpUsed(true);
        users.save(user);
        clearOtpData(user);

        Integer customerId = getCustomerId(user, merchantId);

        // Handle password reset
        if (OTP_TYPE_PASSWORD_RESET.equals(req.getOtpType())) {
            String randomPassword = generateRandomPassword();
            user.setPasswordHash(encoder.encode(randomPassword));
            users.save(user);
            log.info("Password reset with random password for user: {}", maskedPhone);
        }

        log.info("OTP verified successfully for phone: {}", maskedPhone);
        return buildTokens(user, req.getMerchantId(), customerId);
    }

/**
     * Returns the total number of users.
     * @return long - The count of user records.
     */
    @Override
    public long getUserCount() {
        return users.count();
    }
}