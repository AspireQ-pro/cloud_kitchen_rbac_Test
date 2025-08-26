package com.cloudkitchen.rbac.service.impl;

import com.cloudkitchen.rbac.domain.entity.*;
import com.cloudkitchen.rbac.dto.auth.*;
import com.cloudkitchen.rbac.repository.*;
import com.cloudkitchen.rbac.service.AuthService;
import com.cloudkitchen.rbac.service.OtpService;
import com.cloudkitchen.rbac.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserRepository users;
    private final MerchantRepository merchants;
    private final RoleRepository roles;
    private final UserRoleRepository userRoles;
    private final OtpLogRepository otpLogs;
    private final PasswordEncoder encoder;
    private final OtpService otpService;
    private final JwtTokenProvider jwt;

    public AuthServiceImpl(UserRepository users, MerchantRepository merchants, RoleRepository roles,
            UserRoleRepository userRoles, OtpLogRepository otpLogs, PasswordEncoder encoder,
            OtpService otpService, JwtTokenProvider jwt) {
        this.users = users;
        this.merchants = merchants;
        this.roles = roles;
        this.userRoles = userRoles;
        this.otpLogs = otpLogs;
        this.encoder = encoder;
        this.otpService = otpService;
        this.jwt = jwt;
    }

    @Override
    @Transactional
    public AuthResponse registerCustomer(RegisterRequest req) {
        Merchant m = req.getMerchantId() == null ? null
                : merchants.findById(req.getMerchantId())
                        .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));
        User u = new User();
        u.setMerchant(m);
        u.setPhone(req.getPhone());
        u.setUsername(req.getPhone());
        u.setFirstName(req.getFirstName());
        u.setLastName(req.getLastName());
        u.setUserType("customer");
        u.setPasswordHash(encoder.encode(req.getPassword()));
        users.save(u);

        Role customerRole = roles.findByRoleName("customer")
                .orElseThrow(() -> new IllegalStateException("Role 'customer' missing"));
        UserRole ur = new UserRole();
        ur.setUser(u);
        ur.setRole(customerRole);
        ur.setMerchant(m);
        ur.setAssignedAt(LocalDateTime.now());
        userRoles.save(ur);

        return buildTokens(u, req.getMerchantId());
    }

    @Override
    public AuthResponse loginWithPassword(AuthRequest req) {
        User u = loadByPhoneAndMerchant(req.getPhone(), req.getMerchantId());
        if (u.getPasswordHash() == null || !encoder.matches(req.getPassword(), u.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        return buildTokens(u, req.getMerchantId());
    }

    @Override
    @Transactional
    public void requestOtp(OtpRequest req) {
        User u;
        try {
            u = loadByPhoneAndMerchant(req.getPhone(), req.getMerchantId());
        } catch (Exception e) {
            Merchant m = req.getMerchantId() == null ? null
                    : merchants.findById(req.getMerchantId())
                            .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));
            u = new User();
            u.setMerchant(m);
            u.setPhone(req.getPhone());
            u.setUsername(req.getPhone());
            u.setUserType("customer");
            u.setGuest(true);
            users.save(u);

            Role customer = roles.findByRoleName("customer")
                    .orElseThrow(() -> new IllegalStateException("Role 'customer' missing"));
            UserRole ur = new UserRole();
            ur.setUser(u);
            ur.setRole(customer);
            ur.setMerchant(m);
            ur.setAssignedAt(LocalDateTime.now());
            userRoles.save(ur);
        }

        String code = otpService.generateOtp();
        u.setOtpCode(code);
        u.setOtpExpiresAt(LocalDateTime.now().plusMinutes(5));
        u.setOtpAttempts(0);
        users.save(u);

        OtpLog log = new OtpLog();
        log.setMerchant(u.getMerchant());
        log.setPhone(u.getPhone());
        log.setOtpCode(code);
        log.setOtpType(OtpLog.OtpType.LOGIN);
        log.setStatus(OtpLog.OtpStatus.SENT);
        log.setExpiresAt(u.getOtpExpiresAt());
        log.setCreatedOn(LocalDateTime.now());

        otpLogs.save(log);
    }

    @Override
    @Transactional
    public AuthResponse verifyOtp(OtpVerifyRequest req) {
        User u = loadByPhoneAndMerchant(req.getPhone(), req.getMerchantId());
        if (u.getOtpExpiresAt() == null || u.getOtpExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("OTP expired");
        }
        if (u.getOtpAttempts() != null && u.getOtpAttempts() >= 5) {
            throw new IllegalArgumentException("Too many attempts");
        }
        if (!req.getOtp().equals(u.getOtpCode())) {
            u.setOtpAttempts((u.getOtpAttempts() == null ? 0 : u.getOtpAttempts()) + 1);
            users.save(u);
            throw new IllegalArgumentException("Invalid OTP");
        }

        u.setOtpCode(null);
        u.setOtpExpiresAt(null);
        u.setOtpAttempts(0);
        users.save(u);

        return buildTokens(u, req.getMerchantId());
    }

    @Override
    public AuthResponse refresh(RefreshTokenRequest req) {
        Claims claims = jwt.parse(req.getRefreshToken());
        Integer userId = Integer.parseInt(claims.getSubject());
        Integer merchantId = (Integer) claims.get("merchantId");
        User u = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User missing"));
        return buildTokens(u, merchantId);
    }

    private User loadByPhoneAndMerchant(String phone, Integer merchantId) {
        if (merchantId == null) {
            return users.findByPhoneAndMerchantIsNull(phone)
                    .orElseThrow(() -> new IllegalArgumentException("User not found (global)"));
        }
        return users.findByPhoneAndMerchant_MerchantId(phone, merchantId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private AuthResponse buildTokens(User u, Integer merchantId) {
        List<String> roleNames = userRoles.findRoleNames(u.getUserId(), merchantId);
        String access = jwt.createAccessToken(u.getUserId(), merchantId, roleNames);
        String refresh = jwt.createRefreshToken(u.getUserId(), merchantId);
        AuthResponse res = new AuthResponse();
        res.setAccessToken(access);
        res.setRefreshToken(refresh);
        res.setExpiresIn(86400);
        res.setUserId(u.getUserId());
        res.setMerchantId(merchantId);
        res.setRoles(roleNames);
        return res;
    }
}
