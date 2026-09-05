package com.projectmanagement.app.user;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "User not found with id: " + id));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException(
                        "User not found with email: " + email));
    }

    public User createUser(CreateUserRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException(
                    "Email already exists: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(
                        passwordEncoder.encode(
                                request.getPassword()))
                .build();

        return userRepository.save(user);
    }

    public User updateUser(
            Long id,
            UpdateUserRequest request) {

        User existingUser = getUserById(id);

        if (request.getEmail() != null &&
                !request.getEmail().isBlank() &&
                !request.getEmail()
                        .equalsIgnoreCase(existingUser.getEmail())) {

            if (userRepository.existsByEmail(
                    request.getEmail())) {
                throw new RuntimeException(
                        "Email already exists: "
                                + request.getEmail());
            }

            existingUser.setEmail(request.getEmail());
        }

        if (request.getName() != null &&
                !request.getName().isBlank()) {

            existingUser.setName(
                    request.getName());
        }

        if (request.getPassword() != null &&
                !request.getPassword().isBlank()) {

            existingUser.setPassword(
                    passwordEncoder.encode(
                            request.getPassword()));
        }

        return userRepository.save(existingUser);
    }

    public void deleteUser(Long id) {

        User user = getUserById(id);

        user.setDeletedAt(
                java.time.LocalDateTime.now());

        userRepository.save(user);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}