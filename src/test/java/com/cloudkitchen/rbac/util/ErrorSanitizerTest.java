package com.cloudkitchen.rbac.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ErrorSanitizerTest {

    @Test
    void sanitizeErrorMessage_nullReturnsDefault() {
        assertEquals("Invalid request", ErrorSanitizer.sanitizeErrorMessage(null));
    }

    @Test
    void sanitizeErrorMessage_stripsControlAndSymbols() {
        assertEquals("BadRequest", ErrorSanitizer.sanitizeErrorMessage("Bad\nRequest<>"));
    }

    @Test
    void sanitizeLogMessage_nullReturnsPlaceholder() {
        assertEquals("[NULL]", ErrorSanitizer.sanitizeLogMessage(null));
    }

    @Test
    void sanitizeLogMessage_truncatesLongValues() {
        String longMessage = "a".repeat(200);
        String sanitized = ErrorSanitizer.sanitizeLogMessage(longMessage);
        assertTrue(sanitized.length() <= 103);
        assertTrue(sanitized.endsWith("..."));
    }

    @Test
    void sanitizeRejectedValue_masksSensitiveKeywords() {
        assertEquals("[MASKED]", ErrorSanitizer.sanitizeRejectedValue("password=secret"));
    }

    @Test
    void sanitizeRejectedValue_masksPhoneNumbers() {
        assertEquals("****3210", ErrorSanitizer.sanitizeRejectedValue("9876543210"));
    }

    @Test
    void sanitizeRejectedValue_returnsOriginalForSafeValues() {
        assertEquals("safe-value", ErrorSanitizer.sanitizeRejectedValue("safe-value"));
    }
}
