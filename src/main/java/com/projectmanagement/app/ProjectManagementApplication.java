package com.projectmanagement.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ProjectManagementApplication {

    public static void main(String[] args) {
        System.out.println("Starting Project Management Application...");
        SpringApplication.run(
                ProjectManagementApplication.class,
                args);
    }
}