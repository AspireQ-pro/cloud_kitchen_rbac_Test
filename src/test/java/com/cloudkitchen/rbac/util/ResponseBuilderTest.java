package com.cloudkitchen.rbac.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

class ResponseBuilderTest {

    @Test
    void successWithData_includesStandardFields() {
        Map<String, Object> data = Map.of("id", 1);
        Map<String, Object> response = ResponseBuilder.success(200, "OK", data);

        assertEquals(200, response.get("status"));
        assertEquals(200, response.get("code"));
        assertEquals("OK", response.get("message"));
        assertTrue((Boolean) response.get("success"));
        assertEquals(data, response.get("data"));
    }

    @Test
    void successWithoutData_includesStandardFields() {
        Map<String, Object> response = ResponseBuilder.success(201, "Created");

        assertEquals(201, response.get("status"));
        assertEquals(201, response.get("code"));
        assertEquals("Created", response.get("message"));
        assertTrue((Boolean) response.get("success"));
    }

    @Test
    void error_includesStandardFields() {
        Map<String, Object> response = ResponseBuilder.error(400, "Bad request");

        assertEquals(400, response.get("status"));
        assertEquals(400, response.get("code"));
        assertEquals("Bad request", response.get("message"));
        assertFalse((Boolean) response.get("success"));
    }
}
