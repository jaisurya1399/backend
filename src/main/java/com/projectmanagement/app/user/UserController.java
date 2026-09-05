package com.projectmanagement.app.user;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/users")
public class UserController {

        private final UserService userService;

        public UserController(UserService userService) {
                this.userService = userService;
        }

        @GetMapping
        @PreAuthorize("hasAuthority('user.view') or hasRole('ADMIN')")
        public ResponseEntity<List<UserResponse>> getAllUsers() {

                List<UserResponse> users = userService
                                .getAllUsers()
                                .stream()
                                .map(this::toResponse)
                                .toList();

                return ResponseEntity.ok(users);
        }

        @GetMapping("/{id}")
        @PreAuthorize("hasAuthority('user.view') or hasRole('ADMIN')")
        public ResponseEntity<UserResponse> getUserById(
                        @PathVariable Long id) {

                User user = userService.getUserById(id);

                return ResponseEntity.ok(
                                toResponse(user));
        }

        @GetMapping("/email/{email}")
        @PreAuthorize("hasAuthority('user.view') or hasRole('ADMIN')")
        public ResponseEntity<UserResponse> getUserByEmail(
                        @PathVariable String email) {

                User user = userService.getUserByEmail(email);

                return ResponseEntity.ok(
                                toResponse(user));
        }

        @GetMapping("/exists/email/{email}")
        @PreAuthorize("hasAuthority('user.view') or hasRole('ADMIN')")
        public ResponseEntity<Boolean> existsByEmail(
                        @PathVariable String email) {

                return ResponseEntity.ok(
                                userService.existsByEmail(email));
        }

        @PostMapping
        @PreAuthorize("hasAuthority('user.create') or hasRole('ADMIN')")
        public ResponseEntity<UserResponse> createUser(
                        @Valid @RequestBody CreateUserRequest request) {

                User createdUser = userService.createUser(request);

                return ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(toResponse(createdUser));
        }

        @PutMapping("/{id}")
        @PreAuthorize("hasAuthority('user.update') or hasRole('ADMIN')")
        public ResponseEntity<UserResponse> updateUser(
                        @PathVariable Long id,
                        @Valid @RequestBody UpdateUserRequest request) {

                User updatedUser = userService.updateUser(
                                id,
                                request);

                return ResponseEntity.ok(
                                toResponse(updatedUser));
        }

        @DeleteMapping("/{id}")
        @PreAuthorize("hasAuthority('user.delete') or hasRole('ADMIN')")
        public ResponseEntity<Void> deleteUser(
                        @PathVariable Long id) {

                userService.deleteUser(id);

                return ResponseEntity
                                .noContent()
                                .build();
        }

        private UserResponse toResponse(User user) {

                return UserResponse.builder()
                                .id(user.getId())
                                .name(user.getName())
                                .email(user.getEmail())
                                .emailVerifiedAt(
                                                user.getEmailVerifiedAt())
                                .createdAt(
                                                user.getCreatedAt())
                                .updatedAt(
                                                user.getUpdatedAt())
                                .creationToken(
                                                user.getCreationToken())
                                .build();
        }
}