package com.cloudkitchen.rbac.security;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer.FrameOptionsConfig;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    // ✅ Whitelisted endpoints (accessible without authentication)
    private static final String[] AUTH_WHITELIST = {
            "/",
            "/api/v1/auth/signup",
            "/api/v1/auth/customer/login",
            "/api/v1/auth/login",
            "/api/v1/auth/otp/request",
            "/api/v1/auth/otp/verify",
            "/api/v1/auth/refresh",
            "/api/auth/**", // Backward compatibility
            "/error"
    };

    // ✅ Swagger endpoints (accessible without authentication)
    private static final String[] SWAGGER_WHITELIST = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/swagger-ui.html/**",
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/swagger-resources",
            "/swagger-resources/**",
            "/configuration/ui",
            "/configuration/security",
            "/webjars/**",
            "/swagger-config"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter)
            throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .headers(headers -> {
                    headers.frameOptions(FrameOptionsConfig::deny);
                    headers.contentTypeOptions(contentTypeOptions -> {
                    });
                    headers.httpStrictTransportSecurity(hstsConfig -> hstsConfig
                            .maxAgeInSeconds(31536000)
                            .includeSubDomains(true));
                    headers.contentSecurityPolicy(csp -> csp.policyDirectives(
                            "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; font-src 'self' data:"));
                    headers.permissionsPolicy(
                            permissions -> permissions.policy("geolocation=(), camera=(), microphone=()"));
                    headers.referrerPolicy(referrer -> referrer.policy(
                            org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
                })
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/", "/error", "/favicon.ico").permitAll()
                        .requestMatchers(AUTH_WHITELIST).permitAll()
                        .requestMatchers(SWAGGER_WHITELIST).permitAll()
                        .requestMatchers("/api/health/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter()
                                    .write("{\"error\":\"Unauthorized\",\"message\":\"Authentication required\"}");
                        }))
                .addFilterBefore(jwtAuthenticationFilter,
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        String profile = System.getenv("SPRING_PROFILES_ACTIVE");
        String allowedOrigins = System.getenv("CORS_ALLOWED_ORIGINS");

        if ("prod".equals(profile)) {
            if (allowedOrigins == null || allowedOrigins.isEmpty()) {
                throw new IllegalStateException("CORS_ALLOWED_ORIGINS must be set in production");
            }
            configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
            logger.info("CORS configured for production with origins: {}", allowedOrigins);
        } else {
            if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
                configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
                logger.info("CORS configured with specific origins: {}", allowedOrigins);
            } else {
                configuration.setAllowedOrigins(Arrays.asList("http://localhost:3000", "http://localhost:5173"));
                logger.warn("CORS configured for local development (localhost:3000, localhost:5173)");
            }
        }

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L); // Cache preflight for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
