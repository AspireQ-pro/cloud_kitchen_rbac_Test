package com.cloudkitchen.rbac.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.access-valid-seconds}")
    private long accessTokenValiditySeconds;

    @Value("${app.jwt.refresh-valid-seconds}")
    private long refreshTokenValiditySeconds;

    private Key key;

    @PostConstruct
    public void init() {
        // ✅ create signing key from Base64 secret
        this.key = Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    // --- Create Access Token ---
    public String createAccessToken(Integer userId, Integer merchantId, List<String> roles, List<String> permissions) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenValiditySeconds * 1000);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("merchantId", merchantId)
                .claim("roles", roles)
                .claim("permissions", permissions)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // --- Create Refresh Token ---
    public String createRefreshToken(Integer userId, Integer merchantId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshTokenValiditySeconds * 1000);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("merchantId", merchantId)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // --- Parse Token ---
    public Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // --- Validate Token (optional) ---
    public boolean validateToken(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    
    public Integer getUserIdFromToken(String token) {
        Claims claims = parse(token);
        return Integer.valueOf(claims.getSubject());
    }
}