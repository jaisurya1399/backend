package com.projectmanagement.app.auth;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class LoginRateLimitServiceTest {

    @Test
    void blocksAfterConfiguredFailureThreshold() {
        LoginRateLimitService service = new LoginRateLimitService(2, 15);

        service.recordFailure("user@example.com", "127.0.0.1");
        service.recordFailure("user@example.com", "127.0.0.1");

        assertThrows(ResponseStatusException.class,
                () -> service.checkAllowed("user@example.com", "127.0.0.1"));
    }

    @Test
    void successfulLoginClearsFailureState() {
        LoginRateLimitService service = new LoginRateLimitService(2, 15);

        service.recordFailure("user@example.com", "127.0.0.1");
        service.recordSuccess("user@example.com", "127.0.0.1");

        assertDoesNotThrow(() -> service.checkAllowed("user@example.com", "127.0.0.1"));
    }
}
