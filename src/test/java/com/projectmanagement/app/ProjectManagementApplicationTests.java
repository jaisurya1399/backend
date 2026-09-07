package com.projectmanagement.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ProjectManagementApplicationTests {

    @Test
    void contextLoads() {
        // Basic smoke test: fails the build if the Spring context
        // cannot start (bean wiring, config, etc.).
    }
}
