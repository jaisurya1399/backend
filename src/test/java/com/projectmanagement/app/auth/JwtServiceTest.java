package com.projectmanagement.app.auth;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {
    private JwtService jwtService(String issuer) {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secret", "MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=");
        ReflectionTestUtils.setField(service, "jwtExpiration", 60_000L);
        ReflectionTestUtils.setField(service, "issuer", issuer);
        return service;
    }

    @Test
    void generatesAndValidatesAUserToken() {
        JwtService service = jwtService("test-api");
        String token = service.generateToken("user@example.com");

        assertEquals("user@example.com", service.extractUsername(token));
        assertFalse(service.isTokenExpired(token));
    }

    @Test
    void rejectsTokenFromAnotherIssuer() {
        String token = jwtService("issuer-a").generateToken("user@example.com");

        assertThrows(Exception.class, () -> jwtService("issuer-b").extractUsername(token));
    }
}
