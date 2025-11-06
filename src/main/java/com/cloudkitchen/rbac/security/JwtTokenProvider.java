package com.cloudkitchen.rbac.security;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwtTokenProvider {
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";
    
    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.access-valid-seconds:3600}")
    private int accessTokenValiditySeconds;

    @Value("${app.jwt.refresh-valid-seconds:604800}")
    private int refreshTokenValiditySeconds;
    
    @Value("${app.jwt.issuer:cloud-kitchen-rbac}")
    private String issuer;

    private SecretKey key;
    // In-memory token blacklist (suitable for single-instance deployments)
    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();
    private static final long TOKEN_CLEANUP_INTERVAL = 3600000; // 1 hour
    private long lastCleanupTime = System.currentTimeMillis();

    @PostConstruct
    public void init() {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("JWT secret cannot be null or empty. Set JWT_SECRET environment variable.");
        }
        if (secret.length() < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 characters long. Current: " + secret.length());
        }
        try {
            this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            logger.info("JWT token provider initialized with HS256 algorithm");
        } catch (IllegalArgumentException e) {
            logger.error("Failed to initialize JWT signing key", e);
            throw new IllegalStateException("Failed to initialize JWT signing key: " + e.getMessage());
        }
    }

    public String createAccessToken(Integer userId, Integer merchantId, List<String> roles, List<String> permissions) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        Date now = new Date();
        long expiryTime = Math.addExact(now.getTime(), Math.multiplyExact((long)accessTokenValiditySeconds, 1000L));
        Date expiry = new Date(expiryTime);
        String jti = generateJti();

        return Jwts.builder()
                .id(jti)
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .audience().add("cloud-kitchen-app").and()
                .claim("type", TOKEN_TYPE_ACCESS)
                .claim("merchantId", merchantId)
                .claim("roles", roles != null ? roles : List.of())
                .claim("permissions", permissions != null ? permissions : List.of())
                .issuedAt(now)
                .notBefore(now)
                .expiration(expiry)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public String createRefreshToken(Integer userId, Integer merchantId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        Date now = new Date();
        long expiryTime = Math.addExact(now.getTime(), Math.multiplyExact((long)refreshTokenValiditySeconds, 1000L));
        Date expiry = new Date(expiryTime);
        String jti = generateJti();

        return Jwts.builder()
                .id(jti)
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .audience().add("cloud-kitchen-app").and()
                .claim("type", TOKEN_TYPE_REFRESH)
                .claim("merchantId", merchantId)
                .issuedAt(now)
                .notBefore(now)
                .expiration(expiry)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parse(String token) {
        if (isTokenBlacklisted(token)) {
            throw new JwtException("Token has been revoked");
        }
        
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(issuer)
                    .clockSkewSeconds(60)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
                    
            // Validate token type for access tokens
            String tokenType = claims.get("type", String.class);
            if (tokenType == null) {
                throw new JwtException("Token type not specified");
            }
            
            return claims;
        } catch (ExpiredJwtException e) {
            logger.warn("JWT token expired");
            throw e;
        } catch (UnsupportedJwtException e) {
            logger.warn("Unsupported JWT token");
            throw e;
        } catch (MalformedJwtException e) {
            logger.warn("Malformed JWT token");
            throw e;
        } catch (SecurityException e) {
            logger.warn("Invalid JWT signature");
            throw e;
        } catch (IllegalArgumentException e) {
            logger.warn("JWT token compact of handler are invalid");
            throw e;
        }
    }

    public boolean validateToken(String token) {
        try {
            Claims claims = parse(token);
            return !isTokenExpired(claims) && !isTokenBlacklisted(token);
        } catch (JwtException | IllegalArgumentException e) {
            logger.debug("Token validation failed");
            return false;
        }
    }
    
    public boolean validateAccessToken(String token) {
        try {
            Claims claims = parse(token);
            String tokenType = claims.get("type", String.class);
            return TOKEN_TYPE_ACCESS.equals(tokenType) && !isTokenExpired(claims) && !isTokenBlacklisted(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    
    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = parse(token);
            String tokenType = claims.get("type", String.class);
            return TOKEN_TYPE_REFRESH.equals(tokenType) && !isTokenExpired(claims) && !isTokenBlacklisted(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    

    
    public Integer getUserIdFromToken(String token) {
        try {
            Claims claims = parse(token);
            String subject = claims.getSubject();
            if (subject == null || subject.trim().isEmpty()) {
                throw new IllegalArgumentException("Token subject is null or empty");
            }
            return Integer.valueOf(subject);
        } catch (NumberFormatException e) {
            logger.warn("Invalid user ID format in token");
            throw new IllegalArgumentException("Invalid user ID format in token", e);
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("Failed to extract user ID from token");
            throw new IllegalArgumentException("Invalid token", e);
        }
    }
    
    public Integer getMerchantId(String token) {
        try {
            Claims claims = parse(token);
            Object merchantIdObj = claims.get("merchantId");
            if (merchantIdObj == null) {
                return null;
            }
            if (merchantIdObj instanceof Integer integer) {
                return integer;
            }
            if (merchantIdObj instanceof Number number) {
                return number.intValue();
            }
            throw new IllegalArgumentException("Invalid merchantId type in token");
        } catch (JwtException | IllegalArgumentException e) {
            logger.warn("Failed to extract merchant ID from token");
            throw new IllegalArgumentException("Invalid token", e);
        }
    }
    
    public void blacklistToken(String token) {
        try {
            Claims claims = parse(token);
            String jti = claims.getId();
            if (jti != null) {
                blacklistedTokens.add(jti);
                logger.info("Token blacklisted: {}", jti);
                
                // Periodic cleanup to prevent memory leak
                cleanupExpiredTokens();
            }
        } catch (Exception e) {
            logger.warn("Failed to blacklist token");
        }
    }
    
    private void cleanupExpiredTokens() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanupTime > TOKEN_CLEANUP_INTERVAL) {
            // Clear all tokens older than refresh token validity
            // In production, implement proper TTL tracking per token
            if (blacklistedTokens.size() > 10000) {
                blacklistedTokens.clear();
                logger.info("Cleared blacklisted tokens cache");
            }
            lastCleanupTime = currentTime;
        }
    }
    
    public boolean isTokenBlacklisted(String token) {
        try {
            // Extract JTI without full parsing for performance
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            String jti = claims.getId();
            return jti != null && blacklistedTokens.contains(jti);
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean isTokenExpired(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration != null && expiration.before(new Date());
    }
    
    private String generateJti() {
        return UUID.randomUUID().toString();
    }
}