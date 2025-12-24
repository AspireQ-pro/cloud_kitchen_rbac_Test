package com.cloudkitchen.rbac.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.cloudkitchen.rbac.dto.customer.CustomerResponse;
import com.cloudkitchen.rbac.dto.customer.CustomerUpdateRequest;
import com.cloudkitchen.rbac.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerService customerService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "1", authorities = "ROLE_CUSTOMER")
    void testUpdateCustomer_WithProfileDataOnly() throws Exception {
        // Given
        CustomerUpdateRequest request = new CustomerUpdateRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john@example.com");

        CustomerResponse response = new CustomerResponse();
        response.setId(1);
        response.setFirstName("John");
        response.setLastName("Doe");
        response.setEmail("john@example.com");
        response.setUpdatedAt(LocalDateTime.now());

        when(customerService.canAccessCustomer(any(), eq(1))).thenReturn(true);
        when(customerService.updateCustomer(eq(1), any(), any(), eq(1))).thenReturn(response);

        // Create multipart request with JSON data
        MockMultipartFile dataFile = new MockMultipartFile(
            "data", 
            "", 
            MediaType.APPLICATION_JSON_VALUE, 
            objectMapper.writeValueAsBytes(request)
        );

        // When & Then
        mockMvc.perform(multipart("/api/customers/1")
                        .file(dataFile)
                        .with(csrf())
                        .with(req -> {
                            req.setMethod("PATCH");
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.data.lastName").value("Doe"))
                .andExpect(jsonPath("$.data.email").value("john@example.com"));
    }

    @Test
    @WithMockUser(username = "1", authorities = "ROLE_CUSTOMER")
    void testUpdateCustomer_WithProfileImageOnly() throws Exception {
        // Given
        CustomerResponse response = new CustomerResponse();
        response.setId(1);
        response.setFirstName("John");
        response.setLastName("Doe");
        response.setProfileImageKey("1/customer/1/profile_img/profile.jpg");
        response.setUpdatedAt(LocalDateTime.now());

        when(customerService.canAccessCustomer(any(), eq(1))).thenReturn(true);
        when(customerService.updateCustomer(eq(1), any(), any(), eq(1))).thenReturn(response);

        // Create mock image file
        MockMultipartFile imageFile = new MockMultipartFile(
            "profileImage",
            "profile.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "fake image content".getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/api/customers/1")
                        .file(imageFile)
                        .with(csrf())
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.profileImageKey").value("1/customer/1/profile_img/profile.jpg"));
    }

    @Test
    @WithMockUser(username = "1", authorities = "ROLE_CUSTOMER")
    void testUpdateCustomer_WithBothDataAndImage() throws Exception {
        // Given
        CustomerUpdateRequest request = new CustomerUpdateRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john@example.com");
        request.setDob(LocalDate.of(1990, 1, 15));

        CustomerResponse response = new CustomerResponse();
        response.setId(1);
        response.setFirstName("John");
        response.setLastName("Doe");
        response.setEmail("john@example.com");
        response.setDob(LocalDate.of(1990, 1, 15));
        response.setProfileImageKey("1/customer/1/profile_img/profile.jpg");
        response.setUpdatedAt(LocalDateTime.now());

        when(customerService.canAccessCustomer(any(), eq(1))).thenReturn(true);
        when(customerService.updateCustomer(eq(1), any(), any(), eq(1))).thenReturn(response);

        // Create multipart files
        MockMultipartFile dataFile = new MockMultipartFile(
            "data", 
            "", 
            MediaType.APPLICATION_JSON_VALUE, 
            objectMapper.writeValueAsBytes(request)
        );

        MockMultipartFile imageFile = new MockMultipartFile(
            "profileImage",
            "profile.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "fake image content".getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/api/customers/1")
                        .file(dataFile)
                        .file(imageFile)
                        .with(csrf())
                        .with(req -> {
                            req.setMethod("PATCH");
                            return req;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.data.lastName").value("Doe"))
                .andExpect(jsonPath("$.data.email").value("john@example.com"))
                .andExpect(jsonPath("$.data.dob").value("1990-01-15"))
                .andExpect(jsonPath("$.data.profileImageKey").value("1/customer/1/profile_img/profile.jpg"));
    }

    @Test
    @WithMockUser(username = "1", authorities = "ROLE_CUSTOMER")
    void testUpdateCustomer_AccessDenied() throws Exception {
        // Given
        when(customerService.canAccessCustomer(any(), eq(2))).thenReturn(false);

        MockMultipartFile dataFile = new MockMultipartFile(
            "data", 
            "", 
            MediaType.APPLICATION_JSON_VALUE, 
            "{\"firstName\":\"John\"}".getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/api/customers/2")
                        .file(dataFile)
                        .with(csrf())
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "1", authorities = "ROLE_CUSTOMER")
    void testUpdateCustomer_InvalidImageType() throws Exception {
        // Given
        when(customerService.canAccessCustomer(any(), eq(1))).thenReturn(true);
        when(customerService.updateCustomer(eq(1), any(), any(), eq(1)))
            .thenThrow(new RuntimeException("Only JPG, JPEG, and PNG images are allowed"));

        // Create invalid image file
        MockMultipartFile imageFile = new MockMultipartFile(
            "profileImage",
            "profile.gif",
            "image/gif",
            "fake gif content".getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/api/customers/1")
                        .file(imageFile)
                        .with(csrf())
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Only JPG, JPEG, and PNG images are allowed")));
    }

    @Test
    @WithMockUser(username = "1", authorities = "ROLE_CUSTOMER")
    void testUpdateCustomer_ImageTooLarge() throws Exception {
        // Given
        when(customerService.canAccessCustomer(any(), eq(1))).thenReturn(true);
        when(customerService.updateCustomer(eq(1), any(), any(), eq(1)))
            .thenThrow(new RuntimeException("Profile image size cannot exceed 2MB"));

        // Create large image file (simulate)
        MockMultipartFile imageFile = new MockMultipartFile(
            "profileImage",
            "large_profile.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "fake large image content".getBytes()
        );

        // When & Then
        mockMvc.perform(multipart("/api/customers/1")
                        .file(imageFile)
                        .with(csrf())
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Profile image size cannot exceed 2MB")));
    }

    @Test
    @WithMockUser(username = "1", authorities = "ROLE_CUSTOMER")
    void testGetCustomerById_Success() throws Exception {
        // Given
        CustomerResponse response = new CustomerResponse();
        response.setId(1);
        response.setFirstName("John");
        response.setLastName("Doe");
        response.setEmail("john@example.com");
        response.setProfileImageKey("1/customer/1/profile_img/profile.jpg");
        response.setActive(true);
        response.setCreatedAt(LocalDateTime.now());

        when(customerService.canAccessCustomer(any(), eq(1))).thenReturn(true);
        when(customerService.getCustomerById(1)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/customers/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.data.lastName").value("Doe"))
                .andExpect(jsonPath("$.data.email").value("john@example.com"))
                .andExpect(jsonPath("$.data.profileImageKey").value("1/customer/1/profile_img/profile.jpg"))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    @WithMockUser(username = "1", authorities = "ROLE_CUSTOMER")
    void testGetCustomerProfile_Success() throws Exception {
        // Given
        CustomerResponse response = new CustomerResponse();
        response.setId(1);
        response.setFirstName("John");
        response.setLastName("Doe");
        response.setEmail("john@example.com");
        response.setProfileImageKey("1/customer/1/profile_img/profile.jpg");
        response.setMerchantId(1);
        response.setMerchantName("Test Kitchen");

        when(customerService.getCustomerProfile(any())).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/customers/profile")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.data.lastName").value("Doe"))
                .andExpect(jsonPath("$.data.email").value("john@example.com"))
                .andExpect(jsonPath("$.data.profileImageKey").value("1/customer/1/profile_img/profile.jpg"))
                .andExpect(jsonPath("$.data.merchantId").value(1))
                .andExpect(jsonPath("$.data.merchantName").value("Test Kitchen"));
    }

    @Test
    @WithMockUser(username = "1", authorities = "ROLE_CUSTOMER")
    void testUpdateCustomer_EmptyRequest() throws Exception {
        // Given
        CustomerResponse response = new CustomerResponse();
        response.setId(1);
        response.setFirstName("John");
        response.setLastName("Doe");
        response.setUpdatedAt(LocalDateTime.now());

        when(customerService.canAccessCustomer(any(), eq(1))).thenReturn(true);
        when(customerService.updateCustomer(eq(1), any(), any(), eq(1))).thenReturn(response);

        // When & Then - Empty multipart request should still work
        mockMvc.perform(multipart("/api/customers/1")
                        .with(csrf())
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }
}