package com.projectmanagement.app.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.projectmanagement.app.user.User;
import com.projectmanagement.app.user.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class AuthService {

        private final AuthenticationManager authenticationManager;
        private final UserRepository userRepository;
        private final JwtService jwtService;
        private final PasswordEncoder passwordEncoder;

        public AuthService(
                        AuthenticationManager authenticationManager,
                        UserRepository userRepository,
                        JwtService jwtService,
                        PasswordEncoder passwordEncoder) {

                this.authenticationManager = authenticationManager;
                this.userRepository = userRepository;
                this.jwtService = jwtService;
                this.passwordEncoder = passwordEncoder;
        }

        public AuthResponse login(AuthRequest request) {

                Authentication authentication = authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(
                                                request.getEmail(),
                                                request.getPassword()));

                User user = userRepository
                                .findByEmail(request.getEmail())
                                .filter(existingUser -> existingUser.getDeletedAt() == null)
                                .orElseThrow(() -> new UsernameNotFoundException(
                                                "User not found"));

                UserDetails userDetails = (UserDetails) authentication.getPrincipal();

                String token = jwtService.generateToken(userDetails);

                String role = userRepository
                                .findRoleNameByUserId(user.getId())
                                .map(String::toUpperCase)
                                .orElse(null);

                return AuthResponse.builder()
                                .accessToken(token)
                                .tokenType("Bearer")
                                .userId(user.getId())
                                .name(user.getName())
                                .email(user.getEmail())
                                .role(role)
                                .build();
        }

        public AuthMeResponse getCurrentUser(String email) {

                User user = userRepository
                                .findByEmail(email)
                                .filter(existingUser -> existingUser.getDeletedAt() == null)
                                .orElseThrow(() -> new UsernameNotFoundException(
                                                "User not found"));

                String role = userRepository
                                .findRoleNameByUserId(user.getId())
                                .map(String::toUpperCase)
                                .orElse(null);

                return AuthMeResponse.builder()
                                .userId(user.getId())
                                .name(user.getName())
                                .email(user.getEmail())
                                .role(role)
                                .build();
        }

        @Transactional
        public SignupResponse signup(SignupRequest request) {

                String email = request.getEmail()
                                .trim()
                                .toLowerCase();

                // Check existing user
                if (userRepository.findByEmail(email).isPresent()) {
                        throw new RuntimeException(
                                        "User already exists with email: " + email);
                }

                // Create user
                User user = User.builder()
                                .name(request.getName().trim())
                                .email(email)
                                .password(passwordEncoder.encode(request.getPassword()))
                                .build();

                User savedUser = userRepository.save(user);

                return SignupResponse.builder()
                                .userId(savedUser.getId())
                                .name(savedUser.getName())
                                .email(savedUser.getEmail())
                                .message("User registered successfully")
                                .build();
        }
}