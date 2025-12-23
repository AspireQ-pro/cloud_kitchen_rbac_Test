package com.cloudkitchen.rbac.controller;

import com.cloudkitchen.rbac.security.JwtTokenProvider;
import com.cloudkitchen.rbac.service.AuthService;
import com.cloudkitchen.rbac.service.ValidationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class LogoutSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private ValidationService validationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testD001_LogoutWithoutAuthorizationHeader_Returns401() throws Exception {
        // Test case: Call Logout API without Authorization header
        mockMvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Unauthorized - Authentication required."));
    }

    @Test
    void testD001_LogoutWithEmptyAuthorizationHeader_Returns401() throws Exception {
        // Test case: Call Logout API with empty Authorization header
        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Unauthorized - Authentication required."));
    }

    @Test
    void testD001_LogoutWithInvalidAuthorizationFormat_Returns401() throws Exception {
        // Test case: Call Logout API with invalid Authorization header format
        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "InvalidFormat token123")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Unauthorized - Authentication required."));
    }

    @Test
    void testD002_LogoutWithInvalidToken_Returns401() throws Exception {
        // Test case: Call Logout API with invalid/tampered token
        String invalidToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        
        doNothing().when(validationService).validateTokenFormat(anyString());
        when(jwtTokenProvider.validateAccessToken(anyString())).thenReturn(false);

        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer " + invalidToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Unauthorized - Authentication required."));
    }

    @Test
    void testD003_LogoutWithSQLInjectionToken_Returns400() throws Exception {
        // Test case: Call Logout API with SQL injection attempt in token
        String sqlInjectionToken = "' OR 1=1 --";
        
        doThrow(new IllegalArgumentException("Invalid token format"))
                .when(validationService).validateTokenFormat(anyString());

        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer " + sqlInjectionToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Bad Request"));
    }

    @Test
    void testD004_LogoutWithXSSToken_Returns400() throws Exception {
        // Test case: Call Logout API with XSS script in token
        String xssToken = "<script>alert(1)</script>";
        
        doThrow(new IllegalArgumentException("Invalid token format"))
                .when(validationService).validateTokenFormat(anyString());

        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer " + xssToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Bad Request"));
    }

    @Test
    void testValidLogout_Returns200() throws Exception {
        // Test case: Valid logout request
        String validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        
        doNothing().when(validationService).validateTokenFormat(anyString());
        when(jwtTokenProvider.validateAccessToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(anyString())).thenReturn(123);
        doNothing().when(authService).logout(anyInt());

        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer " + validToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    @Test
    void testExpiredToken_Returns401() throws Exception {
        // Test case: Expired token
        String expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        
        doNothing().when(validationService).validateTokenFormat(anyString());
        when(jwtTokenProvider.validateAccessToken(anyString()))
                .thenThrow(new io.jsonwebtoken.ExpiredJwtException(null, null, "Token expired"));

        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer " + expiredToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Unauthorized - Authentication required."));
    }
}