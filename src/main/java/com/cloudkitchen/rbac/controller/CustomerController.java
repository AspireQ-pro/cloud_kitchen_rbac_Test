package com.cloudkitchen.rbac.controller;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cloudkitchen.rbac.constants.ResponseMessages;
import com.cloudkitchen.rbac.dto.common.PageRequest;
import com.cloudkitchen.rbac.dto.common.PageResponse;
import com.cloudkitchen.rbac.dto.customer.CustomerResponse;
import com.cloudkitchen.rbac.dto.customer.CustomerUpdateRequest;
import com.cloudkitchen.rbac.service.CustomerService;
import com.cloudkitchen.rbac.util.HttpResponseUtil;
import com.cloudkitchen.rbac.util.ResponseBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "Customer Management", description = "Customer profile and management operations")
public class CustomerController {

    private final CustomerService customerService;
    
    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PatchMapping(value = "/{id}", consumes = {"multipart/form-data"})
    @Operation(
        summary = "Update Customer Profile",
        description = """
                **QA Testing Guide:**
                
                1. **Authentication:** Login as customer or admin to get JWT token
                2. **Authorization Header:** Bearer {your_jwt_token}
                3. **Path Parameter:** Customer ID (integer)
                4. **Request Body:** multipart/form-data with 'data' (JSON) and optional 'profileImage' (file)
                
                **Test Scenarios:**
                - Valid update with profile data only
                - Valid update with profile data and image
                - Image only update
                - Invalid image type (should fail)
                - Image too large (should fail)
                - Invalid customer ID (404)
                - Unauthorized access (403)
                - Invalid token (401)
                """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Customer updated successfully",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = "{\"status\":200,\"message\":\"Customer updated successfully\",\"data\":{\"customerId\":1,\"name\":\"John Doe\",\"email\":\"john@example.com\"}}"))),
        @ApiResponse(responseCode = "400", description = "Invalid request data or image validation failed"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Access denied"),
        @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Object> updateCustomer(
            @Parameter(description = "Customer ID to update", example = "1", required = true)
            @PathVariable Integer id,
            @Parameter(description = "Customer update request with fields to modify")
            @RequestPart(value = "data", required = false) CustomerUpdateRequest request,
            @Parameter(description = "Profile image file (jpg, jpeg, png only, max 2MB)")
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage,
            Authentication authentication) {
        if (!customerService.canAccessCustomer(authentication, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseBuilder.error(HttpResponseUtil.FORBIDDEN, ResponseMessages.Customer.ACCESS_DENIED_PROFILE));
        }
        try {
            Integer updatedBy = authentication != null ? Integer.valueOf(authentication.getName()) : null;
            CustomerResponse response = customerService.updateCustomer(id, request, profileImage, updatedBy);
            return ResponseEntity.ok(ResponseBuilder.success(HttpResponseUtil.OK, ResponseMessages.Customer.UPDATED_SUCCESS, response));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseBuilder.error(HttpResponseUtil.BAD_REQUEST, ResponseMessages.Customer.UPDATE_FAILED + ": " + e.getMessage()));
        }
    }

    @GetMapping
    @Operation(
        summary = "Get All Customers",
        description = """
                **QA Testing Guide:**

                1. **Authentication:** Login as admin/merchant to get JWT token
                2. **Authorization Header:** Bearer {your_jwt_token}
                3. **Query Parameters:** page, size, status, search (all optional)

                **Test Scenarios:**
                - Default pagination (page=0, size=10)
                - Custom pagination (page=1, size=5)
                - Filter by status (active/inactive)
                - Search by name/email
                - Combined filters
                - Invalid token (401)
                - Insufficient permissions (403)
                """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Customers retrieved successfully",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = "{\"status\":200,\"message\":\"Customers retrieved successfully\",\"data\":{\"content\":[{\"customerId\":1,\"name\":\"John Doe\"}],\"page\":0,\"size\":10,\"totalElements\":1}}"))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Access denied")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Object> getAllCustomers(
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (1-100)", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Filter by customer status", example = "active")
            @RequestParam(required = false) String status,
            @Parameter(description = "Search by name or email", example = "john")
            @RequestParam(required = false) String search,
            Authentication authentication) {
        if (!customerService.canAccessCustomers(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseBuilder.error(HttpResponseUtil.FORBIDDEN, ResponseMessages.Customer.ACCESS_DENIED_LIST));
        }
        try {
            PageRequest pageRequest = new PageRequest(page, size);
            PageResponse<CustomerResponse> customers = customerService.getAllCustomers(pageRequest, status, search, authentication);
            return ResponseEntity.ok(ResponseBuilder.success(HttpResponseUtil.OK, ResponseMessages.Customer.LIST_SUCCESS, customers));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseBuilder.error(HttpResponseUtil.FORBIDDEN, ResponseMessages.Customer.ACCESS_DENIED_LIST));
        }
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Get Customer by ID",
        description = """
                **QA Testing Guide:**

                1. **Authentication:** Login as customer/admin to get JWT token
                2. **Authorization Header:** Bearer {your_jwt_token}
                3. **Path Parameter:** Valid customer ID

                **Test Scenarios:**
                - Valid customer ID (own profile for customers)
                - Invalid customer ID (404)
                - Access other customer's profile (403)
                - No authentication (401)
                """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Customer retrieved successfully",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = "{\"status\":200,\"message\":\"Customer retrieved successfully\",\"data\":{\"customerId\":1,\"name\":\"John Doe\",\"email\":\"john@example.com\",\"phone\":\"+1234567890\"}}"))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Access denied"),
        @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Object> getCustomerById(
            @Parameter(description = "Customer ID to retrieve", example = "1", required = true)
            @PathVariable Integer id,
            Authentication authentication) {
        try {
            CustomerResponse customer = customerService.getCustomerById(id, authentication);
            return ResponseEntity.ok(ResponseBuilder.success(HttpResponseUtil.OK, ResponseMessages.Customer.RETRIEVED_SUCCESS, customer));
        } catch (RuntimeException e) {
            if ("Access denied".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ResponseBuilder.error(HttpResponseUtil.FORBIDDEN, ResponseMessages.Customer.ACCESS_DENIED_VIEW));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseBuilder.error(HttpResponseUtil.NOT_FOUND, ResponseMessages.Customer.NOT_FOUND));
        }
    }
    
    @GetMapping("/profile")
    @Operation(
        summary = "Get Current Customer Profile",
        description = """
                **QA Testing Guide:**

                1. **Authentication:** Login as customer to get JWT token
                2. **Authorization Header:** Bearer {your_jwt_token}
                3. **No Parameters Required**

                **Test Scenarios:**
                - Valid customer token
                - Invalid/expired token (401)
                - Admin token (should work)
                - No token (401)
                """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile retrieved successfully",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = "{\"status\":200,\"message\":\"Profile retrieved successfully\",\"data\":{\"customerId\":1,\"name\":\"John Doe\",\"email\":\"john@example.com\",\"merchantId\":1}}"))),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token"),
        @ApiResponse(responseCode = "404", description = "Profile not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Object> getCustomerProfile(Authentication authentication) {
        try {
            CustomerResponse customer = customerService.getCustomerProfile(authentication);
            return ResponseEntity.ok(ResponseBuilder.success(HttpResponseUtil.OK, ResponseMessages.Customer.PROFILE_RETRIEVED_SUCCESS, customer));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseBuilder.error(HttpResponseUtil.NOT_FOUND, ResponseMessages.Customer.PROFILE_NOT_FOUND));
        }
    }
    
    @GetMapping("/merchant/{merchantId}")
    @Operation(
        summary = "Get Customers by Merchant ID",
        description = """
                **QA Testing Guide:**

                1. **Authentication:** Login as admin/merchant to get JWT token
                2. **Authorization Header:** Bearer {your_jwt_token}
                3. **Path Parameter:** Valid merchant ID
                4. **Query Parameters:** page, size (optional)

                **Test Scenarios:**
                - Valid merchant ID with customers
                - Valid merchant ID with no customers
                - Invalid merchant ID (400)
                - Access other merchant's customers (403)
                - Pagination testing
                - No authentication (401)
                """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Merchant customers retrieved successfully",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = "{\"status\":200,\"message\":\"Merchant customers retrieved successfully\",\"data\":{\"content\":[{\"customerId\":1,\"name\":\"John Doe\",\"merchantId\":1}],\"page\":0,\"size\":10}}"))),
        @ApiResponse(responseCode = "400", description = "Invalid merchant ID"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Access denied")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Object> getCustomersByMerchantId(
            @Parameter(description = "Merchant ID to get customers for", example = "1", required = true)
            @PathVariable Integer merchantId,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (1-100)", example = "10")
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        if (!customerService.canAccessMerchantCustomers(authentication, merchantId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseBuilder.error(HttpResponseUtil.FORBIDDEN, ResponseMessages.Customer.ACCESS_DENIED_MERCHANT_CUSTOMERS));
        }
        try {
            PageRequest pageRequest = new PageRequest(page, size);
            PageResponse<CustomerResponse> customers = customerService.getCustomersByMerchantId(merchantId, pageRequest);
            return ResponseEntity.ok(ResponseBuilder.success(HttpResponseUtil.OK, ResponseMessages.Customer.MERCHANT_CUSTOMERS_SUCCESS, customers));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResponseBuilder.error(HttpResponseUtil.BAD_REQUEST, ResponseMessages.Customer.RETRIEVE_FAILED + ": " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete Customer",
        description = """
                **QA Testing Guide:**

                1. **Authentication:** Login as admin/merchant to get JWT token
                2. **Authorization Header:** Bearer {your_jwt_token}
                3. **Path Parameter:** Customer ID to delete

                **Test Scenarios:**
                - Valid customer ID (soft delete)
                - Invalid customer ID (404)
                - Access other customer's data (403)
                - Invalid token (401)
                """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Customer deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Access denied"),
        @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Object> deleteCustomer(
            @Parameter(description = "Customer ID to delete", example = "1", required = true)
            @PathVariable Integer id,
            Authentication authentication) {
        if (!customerService.canAccessCustomer(authentication, id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ResponseBuilder.error(HttpResponseUtil.FORBIDDEN, ResponseMessages.Customer.ACCESS_DENIED_VIEW));
        }
        try {
            customerService.deleteCustomer(id);
            return ResponseEntity.ok(ResponseBuilder.success(HttpResponseUtil.OK, ResponseMessages.Customer.DELETED_SUCCESS, null));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ResponseBuilder.error(HttpResponseUtil.NOT_FOUND, ResponseMessages.Customer.NOT_FOUND));
        }
    }
}
