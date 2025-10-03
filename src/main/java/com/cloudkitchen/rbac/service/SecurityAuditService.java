package com.cloudkitchen.rbac.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Security audit service for tracking security events
 */
@Service
public class SecurityAuditService {
    
    private static final Logger securityLog = LoggerFactory.getLogger("SECURITY_AUDIT");
    
    public void logSecurityEvent(String eventType, String userId, String details) {
        securityLog.info("SECURITY_EVENT: type={}, userId={}, details={}", 
                        eventType, userId, sanitize(details));
    }
    
    public void logAuthenticationAttempt(String username, String result, String ip) {
        securityLog.info("AUTH_ATTEMPT: username={}, result={}, ip={}", 
                        sanitize(username), result, sanitize(ip));
    }
    
    public void logValidationFailure(String field, String value, String reason) {
        securityLog.warn("VALIDATION_FAILURE: field={}, value={}, reason={}", 
                        field, sanitize(value), reason);
    }
    
    public void logSuspiciousActivity(String activity, String details) {
        securityLog.error("SUSPICIOUS_ACTIVITY: activity={}, details={}", 
                         activity, sanitize(details));
    }
    
    private String sanitize(String input) {
        if (input == null) return "[NULL]";
        return input.replaceAll("[\\r\\n\\t\\x00-\\x1F\\x7F]", "_")
                   .substring(0, Math.min(input.length(), 100));
    }
}