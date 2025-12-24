package com.cloudkitchen.rbac.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.cloudkitchen.rbac.service.AuthService;
import com.cloudkitchen.rbac.util.ResponseBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "Service health and monitoring operations")
public class HealthController {
    private final AuthService authService;

    public HealthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/users")
    @Operation(
        summary = "Get User Count",
        description = "**QA Testing Guide:**\n\n" +
                     "1. **No Authentication Required**\n" +
                     "2. **Response:** Returns total user count in system\n" +
                     "3. **Usage:** Health monitoring and statistics\n\n" +
                     "**Test Scenarios:**\n" +
                     "- Simple GET request\n" +
                     "- Verify response format\n" +
                     "- Check user count accuracy"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User count retrieved successfully",
            content = @Content(mediaType = "application/json",
                examples = @ExampleObject(value = "{\"status\":200,\"message\":\"User count\",\"data\":{\"userCount\":150}}")))  
    })
    public ResponseEntity<Map<String, Object>> getUserCount() {
        long count = authService.getUserCount();
        return ResponseEntity.ok(ResponseBuilder.success(200, "User count", Map.of("userCount", count)));
    }
}