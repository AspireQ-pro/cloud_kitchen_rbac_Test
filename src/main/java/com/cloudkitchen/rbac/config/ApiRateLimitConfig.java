package com.cloudkitchen.rbac.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.lang.NonNull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class ApiRateLimitConfig implements WebMvcConfigurer {
    
    private final RateLimitInterceptor rateLimitInterceptor = new RateLimitInterceptor();

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/auth/**");
    }

    public static class RateLimitInterceptor implements HandlerInterceptor {
        private final ConcurrentHashMap<String, RequestWindow> requestWindows = new ConcurrentHashMap<>();
        private final int maxRequests = 10;
        private final long windowSizeMs = 60000;
        private volatile long lastCleanup = System.currentTimeMillis();
        private final long cleanupIntervalMs = 300000; // 5 minutes

        @Override
        public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
            String clientId = getClientId(request);
            long currentTime = System.currentTimeMillis();
            
            // Periodic cleanup of expired windows
            if (currentTime - lastCleanup > cleanupIntervalMs) {
                cleanupExpiredWindows(currentTime);
                lastCleanup = currentTime;
            }
            
            RequestWindow window = requestWindows.computeIfAbsent(clientId, k -> new RequestWindow());
            
            if (currentTime - window.startTime > windowSizeMs) {
                window.startTime = currentTime;
                window.count.set(0);
            }
            
            if (window.count.incrementAndGet() > maxRequests) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Too many requests\"}");
                return false;
            }
            
            return true;
        }

        private String getClientId(HttpServletRequest request) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            return (xForwardedFor != null && !xForwardedFor.isEmpty()) ? 
                   xForwardedFor.split(",")[0].trim() : request.getRemoteAddr();
        }
        
        private void cleanupExpiredWindows(long currentTime) {
            requestWindows.entrySet().removeIf(entry -> 
                currentTime - entry.getValue().startTime > windowSizeMs * 2);
        }

        private static class RequestWindow {
            volatile long startTime = System.currentTimeMillis();
            final AtomicInteger count = new AtomicInteger(0);
        }
    }
}