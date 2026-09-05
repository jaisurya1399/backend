package com.projectmanagement.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class ProjectManagementApplication {

    public static void main(String[] args) {
        System.out.println("Starting Project Management Application...");
        System.out.println("password: " + new BCryptPasswordEncoder().encode("Admin@123"));
        SpringApplication.run(
                ProjectManagementApplication.class,
                args);
    }
}