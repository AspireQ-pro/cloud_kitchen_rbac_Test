package com.cloudkitchen.rbac.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.cloudkitchen.rbac.dto.common.PageResponse;
import com.cloudkitchen.rbac.dto.merchant.MerchantRequest;
import com.cloudkitchen.rbac.dto.merchant.MerchantResponse;
import com.cloudkitchen.rbac.service.MerchantService;
import com.cloudkitchen.rbac.util.AccessControlUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(MerchantController.class)
class MerchantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MerchantService merchantService;

    @MockBean
    private AccessControlUtil accessControlUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(authorities = "ROLE_SUPER_ADMIN")
    void testGetAllMerchants_WithPagination() throws Exception {
        // Given
        MerchantResponse merchant1 = new MerchantResponse();
        merchant1.setMerchantId(1);
        merchant1.setMerchantName("Merchant 1");

        MerchantResponse merchant2 = new MerchantResponse();
        merchant2.setMerchantId(2);
        merchant2.setMerchantName("Merchant 2");

        PageResponse<MerchantResponse> pageResponse = new PageResponse<>(
                Arrays.asList(merchant1, merchant2),
                0,
                20,
                2
        );

        when(merchantService.getAllMerchants(any(), isNull(), isNull())).thenReturn(pageResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/merchants")
                        .param("page", "0")
                        .param("size", "20")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }

    @Test
    @WithMockUser(authorities = "ROLE_SUPER_ADMIN")
    void testGetAllMerchants_WithFiltering() throws Exception {
        // Given
        MerchantResponse merchant = new MerchantResponse();
        merchant.setMerchantId(1);
        merchant.setMerchantName("Active Merchant");
        merchant.setActive(true);

        PageResponse<MerchantResponse> pageResponse = new PageResponse<>(
                Arrays.asList(merchant),
                0,
                20,
                1
        );

        when(merchantService.getAllMerchants(any(), eq("active"), isNull())).thenReturn(pageResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/merchants")
                        .param("page", "0")
                        .param("size", "20")
                        .param("status", "active")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @WithMockUser(authorities = "ROLE_SUPER_ADMIN")
    void testCreateMerchant_Success() throws Exception {
        // Given
        MerchantRequest request = new MerchantRequest();
        request.setMerchantName("New Merchant");
        request.setPhone("1234567890");
        request.setEmail("new@example.com");
        request.setUsername("newuser");
        request.setPassword("Password123!");

        MerchantResponse response = new MerchantResponse();
        response.setMerchantId(1);
        response.setMerchantName("New Merchant");

        when(merchantService.createMerchant(any(MerchantRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.merchantId").value(1))
                .andExpect(jsonPath("$.data.merchantName").value("New Merchant"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_SUPER_ADMIN")
    void testGetMerchantById_Success() throws Exception {
        // Given
        MerchantResponse response = new MerchantResponse();
        response.setMerchantId(1);
        response.setMerchantName("Test Merchant");

        when(merchantService.getMerchantById(1)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/v1/merchants/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.merchantId").value(1))
                .andExpect(jsonPath("$.data.merchantName").value("Test Merchant"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_SUPER_ADMIN")
    void testGetMerchantById_NotFound() throws Exception {
        // Given
        when(merchantService.getMerchantById(999))
                .thenThrow(new com.cloudkitchen.rbac.exception.BusinessExceptions.MerchantNotFoundException("Merchant not found"));

        // When & Then
        mockMvc.perform(get("/api/v1/merchants/999")
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }
}

