package com.cloudkitchen.rbac.controller;

import com.cloudkitchen.rbac.dto.auth.AuthRequest;
import com.cloudkitchen.rbac.dto.auth.OtpVerifyRequest;
import com.cloudkitchen.rbac.security.JwtTokenProvider;
import com.cloudkitchen.rbac.security.SecurityConfig;
import com.cloudkitchen.rbac.service.AuthService;
import com.cloudkitchen.rbac.service.ValidationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

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
    void verifyOtp_WithMissingPhoneNumber_ShouldReturnMobileNumberRequiredError() throws Exception {
        // Arrange
        OtpVerifyRequest request = new OtpVerifyRequest();
        request.setMerchantId(1);
        request.setOtp("1234");
        // phone is intentionally not set (null)

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/otp/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Mobile number is required"))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void verifyOtp_WithEmptyPhoneNumber_ShouldReturnMobileNumberRequiredError() throws Exception {
        // Arrange
        OtpVerifyRequest request = new OtpVerifyRequest();
        request.setMerchantId(1);
        request.setPhone(""); // empty phone
        request.setOtp("1234");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/otp/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Mobile number is required"))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void verifyOtp_WithWhitespaceOnlyPhoneNumber_ShouldReturnMobileNumberRequiredError() throws Exception {
        // Arrange
        OtpVerifyRequest request = new OtpVerifyRequest();
        request.setMerchantId(1);
        request.setPhone("   "); // whitespace only
        request.setOtp("1234");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/otp/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Mobile number is required"))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void merchantLogin_WithIncorrectContentType_ShouldReturn415UnsupportedMediaType() throws Exception {
        // Arrange
        AuthRequest request = new AuthRequest();
        request.setUsername("merchant@test.com");
        request.setPassword("password123");
        request.setMerchantId(0);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.TEXT_PLAIN) // Wrong content type
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.message").value("Content-Type must be application/json"))
                .andExpect(jsonPath("$.code").value(415));
    }

    @Test
    void customerLogin_WithIncorrectContentType_ShouldReturn415UnsupportedMediaType() throws Exception {
        // Arrange
        AuthRequest request = new AuthRequest();
        request.setUsername("customer@test.com");
        request.setPassword("password123");
        request.setMerchantId(1);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/customer/login")
                .contentType(MediaType.TEXT_PLAIN) // Wrong content type
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.message").value("Content-Type must be application/json"))
                .andExpect(jsonPath("$.code").value(415));
    }
}