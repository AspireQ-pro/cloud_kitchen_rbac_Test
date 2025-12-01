package com.cloudkitchen.rbac.integration;
import com.cloudkitchen.rbac.dto.merchant.MerchantRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MerchantControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(authorities = "ROLE_SUPER_ADMIN")
    void testGetAllMerchants_WithPagination() throws Exception {
        mockMvc.perform(get("/api/v1/merchants")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "merchantName")
                        .param("sortDirection", "asc")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.page").exists())
                .andExpect(jsonPath("$.data.size").exists())
                .andExpect(jsonPath("$.data.totalElements").exists());
    }

    @Test
    @WithMockUser(authorities = "ROLE_SUPER_ADMIN")
    void testGetAllMerchants_WithSearch() throws Exception {
        mockMvc.perform(get("/api/v1/merchants")
                        .param("page", "0")
                        .param("size", "10")
                        .param("search", "test")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = "ROLE_SUPER_ADMIN")
    void testCreateMerchant_Success() throws Exception {
        MerchantRequest request = new MerchantRequest();
        request.setMerchantName("Integration Test Merchant");
        request.setPhone("9876543210");
        request.setEmail("integration@test.com");
        request.setUsername("integrationuser");
        request.setPassword("Password123!");

        mockMvc.perform(post("/api/v1/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists());
    }
}

