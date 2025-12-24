package com.cloudkitchen.rbac.filter;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.cloudkitchen.rbac.util.ResponseBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ContentTypeValidationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ContentTypeValidationFilter.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod();
        String requestURI = request.getRequestURI();

        // Only validate POST requests to auth endpoints
        if ("POST".equals(method) && requestURI.contains("/api/") && requestURI.contains("/auth/")) {
            String contentType = request.getContentType();
            
            // Check if content type is missing or not application/json
            if (contentType == null || !contentType.toLowerCase().startsWith("application/json")) {
                logger.warn("Invalid content type '{}' for endpoint '{}'", contentType, requestURI);
                
                response.setStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                
                Map<String, Object> errorResponse = ResponseBuilder.error(415, "Content-Type must be application/json");
                errorResponse.put("path", requestURI);
                
                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}