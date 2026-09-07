package com.projectmanagement.app.auth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
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
        private final RefreshTokenRepository refreshTokenRepository;
        private final RevokedAccessTokenRepository revokedAccessTokenRepository;
        @Value("${auth.refresh-expiration-days:30}")
        private long refreshExpirationDays;

        public AuthService(
                        AuthenticationManager authenticationManager,
                        UserRepository userRepository,
                        JwtService jwtService,
                        PasswordEncoder passwordEncoder,
                        RefreshTokenRepository refreshTokenRepository,
                        RevokedAccessTokenRepository revokedAccessTokenRepository) {

                this.authenticationManager = authenticationManager;
                this.userRepository = userRepository;
                this.jwtService = jwtService;
                this.passwordEncoder = passwordEncoder;
                this.refreshTokenRepository = refreshTokenRepository;
                this.revokedAccessTokenRepository = revokedAccessTokenRepository;
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
                String refreshToken = createRefreshToken(user);

                String role = userRepository
                                .findRoleNameByUserId(user.getId())
                                .map(String::toUpperCase)
                                .orElse(null);

                return AuthResponse.builder()
                                .accessToken(token)
                                .refreshToken(refreshToken)
                                .tokenType("Bearer")
                                .userId(user.getId())
                                .name(user.getName())
                                .email(user.getEmail())
                                .role(role)
                                .build();
        }

        public AuthResponse refresh(RefreshRequest request) {
                RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(request.getRefreshToken()))
                                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
                if (stored.getRevokedAt() != null || !stored.getExpiresAt().isAfter(LocalDateTime.now())
                                || stored.getUser().getDeletedAt() != null)
                        throw new RuntimeException("Invalid refresh token");
                stored.setRevokedAt(LocalDateTime.now());
                refreshTokenRepository.save(stored);
                User user = stored.getUser();
                String accessToken = jwtService.generateToken(user.getEmail());
                return AuthResponse.builder().accessToken(accessToken).refreshToken(createRefreshToken(user))
                                .tokenType("Bearer")
                                .userId(user.getId()).name(user.getName()).email(user.getEmail())
                                .role(userRepository.findRoleNameByUserId(user.getId()).map(String::toUpperCase)
                                                .orElse(null))
                                .build();
        }

        public void logout(String accessToken, LogoutRequest request) {
                refreshTokenRepository.findByTokenHash(hash(request.getRefreshToken())).ifPresent(token -> {
                        token.setRevokedAt(LocalDateTime.now());
                        refreshTokenRepository.save(token);
                });
                String tokenId = jwtService.extractTokenId(accessToken);
                revokedAccessTokenRepository.save(RevokedAccessToken.builder().tokenId(tokenId)
                                .expiresAt(jwtService.extractExpiration(accessToken).toInstant()
                                                .atZone(ZoneId.systemDefault()).toLocalDateTime())
                                .build());
        }

        private String createRefreshToken(User user) {
                byte[] bytes = new byte[48];
                new SecureRandom().nextBytes(bytes);
                String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
                refreshTokenRepository.save(RefreshToken.builder().user(user).tokenHash(hash(token))
                                .expiresAt(LocalDateTime.now().plusDays(refreshExpirationDays)).build());
                return token;
        }

        private String hash(String value) {
                try {
                        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                                        .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                } catch (NoSuchAlgorithmException exception) {
                        throw new IllegalStateException(exception);
                }
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
