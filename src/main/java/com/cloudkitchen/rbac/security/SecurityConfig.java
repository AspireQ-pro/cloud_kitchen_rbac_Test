package com.cloudkitchen.rbac.security;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Value("${cors.allowed.origins:}")
    
    private String allowedOrigins;

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

        // Check environment variable first, then property
        String corsOrigins = System.getenv("CORS_ALLOWED_ORIGINS");
        if (corsOrigins == null || corsOrigins.isEmpty()) {
            corsOrigins = allowedOrigins;
        }

        if ("prod".equals(activeProfile)) {
            if (corsOrigins == null || corsOrigins.isEmpty() || "*".equals(corsOrigins)) {
                throw new IllegalStateException("CORS wildcard (*) not allowed in production. Set CORS_ALLOWED_ORIGINS environment variable");
            }
            configuration.setAllowedOrigins(Arrays.asList(corsOrigins.split(",")));
            logger.info("CORS configured for production with origins: {}", corsOrigins);
        } else {
            if (corsOrigins != null && !corsOrigins.isEmpty() && !"*".equals(corsOrigins)) {
                configuration.setAllowedOrigins(Arrays.asList(corsOrigins.split(",")));
                logger.info("CORS configured with specific origins: {}", corsOrigins);
            } else {
                configuration.setAllowedOriginPatterns(Arrays.asList("*"));
                logger.warn("CORS configured for development - allowing all origins (*)");
            }
        }

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
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
