package com.cloudkitchen.rbac.security;

import io.jsonwebtoken.Claims;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Collections;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try {
                Claims claims = jwtTokenProvider.parse(token);
                String userId = claims.getSubject();
                
                // Extract roles and permissions for authorities
                List<String> roles = extractStringList(claims, "roles");
                List<String> permissions = extractStringList(claims, "permissions");

                List<SimpleGrantedAuthority> authorities = permissions != null ?
                    permissions.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList()) :
                    Collections.emptyList();

                // Add role-based authorities with ROLE_ prefix for Spring Security
                if (roles != null) {
                    authorities.addAll(roles.stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                        .collect(Collectors.toList()));
                }

                if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    userId, null, authorities);
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (io.jsonwebtoken.ExpiredJwtException e) {
                logger.warn("JWT token expired: {}", e.getMessage(), e);
            } catch (io.jsonwebtoken.MalformedJwtException e) {
                logger.warn("Malformed JWT token: {}", e.getMessage(), e);
            } catch (io.jsonwebtoken.security.SignatureException e) {
                logger.warn("Invalid JWT signature: {}", e.getMessage(), e);
            } catch (Exception e) {
                logger.warn("JWT Authentication failed: {}", e.getMessage(), e);
            }
        }

        filterChain.doFilter(request, response);
    }

    private List<String> extractStringList(Claims claims, String key) {
        Object value = claims.get(key);
        if (value instanceof List<?>) {
            List<?> list = (List<?>) value;
            return list.stream()
                    .filter(item -> item instanceof String)
                    .map(String.class::cast)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
