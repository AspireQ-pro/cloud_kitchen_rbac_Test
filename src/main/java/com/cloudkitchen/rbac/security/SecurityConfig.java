package com.cloudkitchen.rbac.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    // ✅ Whitelisted endpoints (accessible without authentication)
    private static final String[] AUTH_WHITELIST = {
        "/",
        "/api/auth/register",
        "/api/auth/login",
        "/api/auth/merchant/login",
        "/api/auth/otp/request",
        "/api/auth/otp/verify",
        "/api/auth/refresh"
    };

    // ✅ Swagger endpoints (accessible without authentication)
    private static final String[] SWAGGER_WHITELIST = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/swagger-resources",
            "/webjars/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // ✅ new syntax
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(AUTH_WHITELIST).permitAll()   // ✅ replaced antMatchers
                        .requestMatchers(SWAGGER_WHITELIST).permitAll() // ✅ replaced antMatchers
                        .requestMatchers("/api/auth/**").permitAll()    // ✅ Allow all auth endpoints
                        .anyRequest().authenticated() // everything else needs login
                )
                .addFilterBefore(jwtAuthenticationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
